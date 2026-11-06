package it.barizone.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import it.barizone.support.IntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

class AccountIntegrationTest extends IntegrationTest {

    private static final String PASSWORD = "Password-Valida-1";

    @Test
    void userMustConfirmEmailBeforeLogin() throws Exception {
        mvc.perform(json(post("/api/account/register"), userJson("Anna@Example.it", PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.verification_required").value(true))
                .andExpect(jsonPath("$.email_status").value("accepted"));
        assertThat(email.batches.getFirst().getFirst().to()).containsExactly("anna@example.it");
        assertThat(jdbc.queryForObject("SELECT password_hash FROM user_accounts", String.class)).startsWith("$2").doesNotContain(PASSWORD);

        String login = "{\"email\":\"anna@example.it\",\"password\":\"" + PASSWORD + "\"}";
        mvc.perform(json(post("/api/account/login"), login)).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("email_not_verified"));

        mvc.perform(json(post("/api/account/verify-email"), "{\"token\":\"" + lastEmailToken() + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("user"));
        // il link è monouso
        mvc.perform(json(post("/api/account/verify-email"), "{\"token\":\"" + lastEmailToken() + "\"}")).andExpect(status().isBadRequest());

        MvcResult ok = mvc.perform(json(post("/api/account/login"), login)).andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("user")).andExpect(jsonPath("$.business").doesNotExist()).andReturn();
        String setCookie = ok.getResponse().getHeader("Set-Cookie");
        assertThat(setCookie).startsWith("__Host-barizone_account=").contains("HttpOnly", "Secure", "SameSite=Lax", "Max-Age=1209600");
        Cookie session = cookie(ok, "__Host-barizone_account");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM account_sessions WHERE token_hash = ?", Long.class, session.getValue())).isZero();

        mvc.perform(get("/api/account/me").cookie(session)).andExpect(jsonPath("$.email").value("anna@example.it"));
        mvc.perform(post("/api/account/logout").header("Origin", ORIGIN).cookie(session)).andExpect(status().isOk());
        mvc.perform(get("/api/account/me").cookie(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void existingEmailGetsTheSameAnswerAndNoSecondAccount() throws Exception {
        userCookie("anna@example.it", PASSWORD);
        email.reset();
        mvc.perform(json(post("/api/account/register"), userJson("anna@example.it", "Altra-Password-9")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.ok").value(true));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_accounts", Long.class)).isEqualTo(1);
        assertThat(email.batches).as("nessuna email a chi è già attivo").isEmpty();
    }

    @Test
    void registrationValidatesEverything() throws Exception {
        mvc.perform(json(post("/api/account/register"), userJson("non-una-email", PASSWORD)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value("Inserisci un indirizzo email valido."));
        mvc.perform(json(post("/api/account/register"), userJson("a@example.it", "corta1")))
                .andExpect(jsonPath("$.detail").value("La password deve avere almeno 10 caratteri."));
        mvc.perform(json(post("/api/account/register"), userJson("a@example.it", "SoloLettereQui")))
                .andExpect(jsonPath("$.detail").value("La password deve contenere almeno una lettera e un numero."));
        mvc.perform(json(post("/api/account/register"), userJson("a@example.it", "x".repeat(60) + "1" + "è".repeat(10))))
                .andExpect(jsonPath("$.detail").value("La password è troppo lunga (massimo 72 caratteri)."));
        mvc.perform(json(post("/api/account/register"), userJson("a@example.it", PASSWORD).replace("\"accept_terms\":true", "\"accept_terms\":false")))
                .andExpect(jsonPath("$.detail").value("Per registrarti devi accettare le condizioni e l’informativa sulla privacy."));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_accounts", Long.class)).isZero();
    }

    @Test
    void wrongPasswordsAreCountedAndLockAfterTen() throws Exception {
        userCookie("anna@example.it", PASSWORD);
        for (int i = 0; i < 10; i++) {
            mvc.perform(json(post("/api/account/login"), "{\"email\":\"anna@example.it\",\"password\":\"Sbagliata-123\"}"))
                    .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.detail").value("Email o password non corrette."));
        }
        mvc.perform(json(post("/api/account/login"), "{\"email\":\"anna@example.it\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isTooManyRequests());
        // Un'email inesistente riceve lo stesso messaggio di una password errata.
        mvc.perform(json(post("/api/account/login"), "{\"email\":\"nessuno@example.it\",\"password\":\"Sbagliata-123\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.detail").value("Email o password non corrette."));
    }

    @Test
    void passwordResetWorksOnceAndClosesOtherSessions() throws Exception {
        Cookie old = userCookie("anna@example.it", PASSWORD);
        email.reset();
        mvc.perform(json(post("/api/account/forgot-password"), "{\"email\":\"sconosciuta@example.it\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ok").value(true));
        assertThat(email.batches).as("nessuna email per indirizzi sconosciuti").isEmpty();

        mvc.perform(json(post("/api/account/forgot-password"), "{\"email\":\"anna@example.it\"}")).andExpect(status().isOk());
        String token = lastEmailToken();
        mvc.perform(json(post("/api/account/reset-password"), "{\"token\":\"" + token + "\",\"password\":\"corta\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(json(post("/api/account/reset-password"), "{\"token\":\"" + token + "\",\"password\":\"Nuova-Password-77\"}"))
                .andExpect(status().isOk());
        mvc.perform(json(post("/api/account/reset-password"), "{\"token\":\"" + token + "\",\"password\":\"Ancora-Password-88\"}"))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/account/me").cookie(old)).andExpect(status().isUnauthorized());
        mvc.perform(json(post("/api/account/login"), "{\"email\":\"anna@example.it\",\"password\":\"" + PASSWORD + "\"}")).andExpect(status().isUnauthorized());
        mvc.perform(json(post("/api/account/login"), "{\"email\":\"anna@example.it\",\"password\":\"Nuova-Password-77\"}")).andExpect(status().isOk());
    }

    @Test
    void registrationIsRateLimitedPerAddress() throws Exception {
        for (int i = 0; i < 10; i++) {
            mvc.perform(json(post("/api/account/register"), userJson("utente" + i + "@example.it", PASSWORD))).andExpect(status().isCreated());
        }
        mvc.perform(json(post("/api/account/register"), userJson("utente11@example.it", PASSWORD))).andExpect(status().isTooManyRequests());
    }

    @Test
    void threeAccessTypesAreSeparate() throws Exception {
        Cookie user = userCookie("anna@example.it", PASSWORD);
        Cookie merchant = approvedMerchantCookie("mario@example.it", PASSWORD, "Panificio Rossi", vat("0123456789"));
        Cookie admin = adminCookie();

        mvc.perform(get("/api/admin/news").cookie(user)).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/news").cookie(merchant)).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/news").cookie(admin)).andExpect(status().isOk());

        mvc.perform(get("/api/account/business").cookie(user)).andExpect(status().isForbidden());
        mvc.perform(get("/api/account/business")).andExpect(status().isForbidden());
        mvc.perform(get("/api/account/business").cookie(merchant)).andExpect(status().isOk())
                .andExpect(jsonPath("$.business_name").value("Panificio Rossi"));
        // un cookie di sessione admin non vale come account attività
        mvc.perform(get("/api/account/business").cookie(admin)).andExpect(status().isForbidden());
    }
}
