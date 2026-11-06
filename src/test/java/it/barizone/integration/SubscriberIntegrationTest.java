package it.barizone.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import it.barizone.support.IntegrationTest;
import org.junit.jupiter.api.Test;

class SubscriberIntegrationTest extends IntegrationTest {

    private static final String SIGNUP = """
            {"first_name":"Maria","last_name":"Rossi","email":"Maria.Rossi@Example.it","phone":"+39 080 1234567",
             "quartiere":"Carrassi","consent":true,"consent_version":"barizone-updates-v1"}""";

    @Test
    void signupSavesConsentAndSendsTwoIdempotentEmails() throws Exception {
        String body = mvc.perform(json(post("/api/subscribers"), SIGNUP))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email_status").value("accepted"))
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(body, "$.token");
        assertThat(email.batches).hasSize(1);
        assertThat(email.batches.getFirst()).hasSize(2);
        assertThat(email.batches.getFirst().get(0).to()).containsExactly("maria.rossi@example.it");
        assertThat(email.batches.getFirst().get(1).to()).containsExactly("info@barizone.it");
        assertThat(email.keys.getFirst()).startsWith("news-signup/");

        // Seconda iscrizione con la stessa email: risposta neutra, nessuna email.
        mvc.perform(json(post("/api/subscribers"), SIGNUP)).andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Richiesta registrata. Se già iscritto, i dati precedenti restano invariati."));
        assertThat(email.batches).hasSize(1);

        mvc.perform(get("/api/admin/subscribers").cookie(adminCookie()))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].consent_version").value("barizone-updates-v1"))
                .andExpect(jsonPath("$[0].push_requested").value(1))
                .andExpect(jsonPath("$[0].push_devices").value(0));

        mvc.perform(json(post("/api/subscribers/unsubscribe"), "{\"token\":\"" + token + "\"}")).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT status FROM news_subscribers", String.class)).isEqualTo("unsubscribed");
    }

    @Test
    void signupIsSavedEvenIfEmailProviderFails() throws Exception {
        email.result = "failed";
        mvc.perform(json(post("/api/subscribers"), SIGNUP)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.email_status").value("failed"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM news_subscribers", Long.class)).isEqualTo(1);
    }

    @Test
    void honeypotMissingConsentAndBadVersionAreRejected() throws Exception {
        mvc.perform(json(post("/api/subscribers"), SIGNUP.replace("\"consent\":true", "\"consent\":true,\"website\":\"http://spam\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Controlla nome, cognome, email e consenso."));
        mvc.perform(json(post("/api/subscribers"), SIGNUP.replace("\"consent\":true", "\"consent\":\"si\"")))
                .andExpect(status().isBadRequest());
        mvc.perform(json(post("/api/subscribers"), SIGNUP.replace("barizone-updates-v1", "v0")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value("Versione del consenso non valida."));
        mvc.perform(get("/api/subscribers/config")).andExpect(jsonPath("$.contact_email").value("info@barizone.it"));
    }
}
