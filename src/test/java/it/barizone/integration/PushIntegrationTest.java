package it.barizone.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import it.barizone.support.IntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

class PushIntegrationTest extends IntegrationTest {

    /** Le campagne accettano solo link https: le richieste admin simulano il sito in HTTPS. */
    private static MockHttpServletRequestBuilder https(MockHttpServletRequestBuilder builder, String body) {
        return builder.secure(true).with(r -> {
            r.setScheme("https");
            r.setServerPort(443);
            return r;
        }).header("Origin", "https://localhost")
                .contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private void subscribe(String suffix, String quartiere) throws Exception {
        mvc.perform(json(post("/api/push/subscription"), "{\"marketing_consent\":true,\"consent_version\":\"marketing-v1\","
                        + "\"quartiere\":\"" + quartiere + "\",\"subscription\":" + subscriptionJson(suffix) + "}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.saved").value(true));
    }

    private String campaign(Cookie admin, String body) throws Exception {
        return JsonPath.read(mvc.perform(https(post("/api/admin/push"), body).cookie(admin))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
    }

    @Test
    void keysAreStableAndSubscriptionsValidated() throws Exception {
        String key = JsonPath.read(mvc.perform(get("/api/push/key")).andReturn().getResponse().getContentAsString(), "$.publicKey");
        assertThat(it.barizone.util.Tokens.fromBase64Url(key)).hasSize(65);
        mvc.perform(get("/api/push/key")).andExpect(jsonPath("$.publicKey").value(key));

        mvc.perform(json(post("/api/push/subscription"), "{\"marketing_consent\":true,\"consent_version\":\"marketing-v1\","
                        + "\"quartiere\":\"Murat\",\"subscription\":" + subscriptionJson("x").replace("fcm.googleapis.com", "evil.example") + "}"))
                .andExpect(status().isBadRequest());
        mvc.perform(json(post("/api/push/subscription"), "{\"marketing_consent\":false,\"consent_version\":\"marketing-v1\","
                        + "\"quartiere\":\"Murat\",\"subscription\":" + subscriptionJson("x") + "}"))
                .andExpect(status().isBadRequest());

        subscribe("dev1", "Murat");
        String endpoint = "{\"endpoint\":\"https://fcm.googleapis.com/fcm/send/dev1\"}";
        mvc.perform(json(post("/api/push/status"), endpoint)).andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.quartiere").value("Murat"));
        mvc.perform(json(delete("/api/push/subscription"), endpoint)).andExpect(jsonPath("$.revoked").value(true));
        mvc.perform(json(post("/api/push/status"), endpoint)).andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void sponsoredCampaignNeedsPaymentAndConfirmation() throws Exception {
        Cookie admin = adminCookie();
        String body = "{\"title\":\"Saldi\",\"body\":\"Sconti in via Sparano\",\"url\":\"/aziende\",\"quartiere\":\"Murat\","
                + "\"business\":\"Negozio\",\"contract_ref\":\"C-1\",\"amount_cents\":5000,\"payment_status\":\"pending\"}";
        String id = campaign(admin, body);
        mvc.perform(https(post("/api/admin/push/" + id + "/send"), "{\"confirm\":true}").cookie(admin))
                .andExpect(status().isBadRequest());
        mvc.perform(https(put("/api/admin/push/" + id), body.replace("pending", "paid")).cookie(admin)).andExpect(status().isOk());
        mvc.perform(https(post("/api/admin/push/" + id + "/send"), "{\"confirm\":false}").cookie(admin))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value("Conferma esplicita richiesta per l’invio"));
        mvc.perform(https(post("/api/admin/push"), body.replace("/aziende", "javascript:x")).cookie(admin))
                .andExpect(status().isBadRequest());
    }

    @Test
    void campaignReachesDistrictOnceAndRemovesExpiredDevices() throws Exception {
        subscribe("murat1", "Murat");
        subscribe("murat2", "Murat");
        subscribe("citta", "Tutta Bari");
        subscribe("japigia", "Japigia");
        Cookie admin = adminCookie();
        String id = campaign(admin, "{\"kind\":\"barizone\",\"title\":\"Avviso\",\"body\":\"Strade chiuse\",\"url\":\"/notizie\","
                + "\"quartiere\":\"Murat\",\"payment_status\":\"pending\"}");

        mvc.perform(https(post("/api/admin/push/" + id + "/send"), "{\"confirm\":true}").cookie(admin))
                .andExpect(jsonPath("$.accepted").value(3)).andExpect(jsonPath("$.more").value(false));
        assertThat(push.messages).allSatisfy(m -> assertThat(m.url()).isEqualTo("https://localhost/notizie"));
        // Nessun doppio invio
        mvc.perform(https(post("/api/admin/push/" + id + "/send"), "{\"confirm\":true}").cookie(admin))
                .andExpect(jsonPath("$.accepted").value(0));
        assertThat(push.targets).hasSize(3);

        mvc.perform(get("/api/admin/push").cookie(admin))
                .andExpect(jsonPath("$.campaigns[0].status").value("completed"))
                .andExpect(jsonPath("$.campaigns[0].accepted").value(3))
                .andExpect(jsonPath("$.campaigns[0].kind").value("barizone"))
                .andExpect(jsonPath("$.total").value(4));

        // Un dispositivo che risponde 410 viene rimosso
        push.status.set(410);
        String second = campaign(admin, "{\"kind\":\"barizone\",\"title\":\"Due\",\"body\":\"Testo\",\"url\":\"/\","
                + "\"quartiere\":\"Japigia\",\"payment_status\":\"pending\"}");
        mvc.perform(https(post("/api/admin/push/" + second + "/send"), "{\"confirm\":true}").cookie(admin))
                .andExpect(jsonPath("$.failed").value(2));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM push_subscriptions", Long.class)).isEqualTo(2);
    }
}
