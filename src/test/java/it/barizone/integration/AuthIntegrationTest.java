package it.barizone.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import it.barizone.support.IntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class AuthIntegrationTest extends IntegrationTest {

    private static final String ACCOUNT = """
            INSERT INTO admin_accounts (username, name, salt, password_hash, iterations)
            VALUES ('redazione', 'Redazione', 'a71473db422fdf6d497dc31e1cfbef9e73202215495396d5',
                    '67d94092a0375ee4a2790f02bc2dd6013d5ac05b1a20d043211c7f6e901976af', 100000)
            ON CONFLICT DO NOTHING""";

    @Test
    void passwordOnlyLoginSetsSecureHostCookie() throws Exception {
        MvcResult r = mvc.perform(json(post("/api/auth/login"), "{\"password\":\"password-di-servizio\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("admin"))
                .andExpect(jsonPath("$.email").value("admin@example.test"))
                .andReturn();
        String setCookie = r.getResponse().getHeader("Set-Cookie");
        assertThat(setCookie).startsWith("__Host-barizone_session=").contains("HttpOnly", "Secure", "SameSite=Lax", "Path=/", "Max-Age=43200");

        mvc.perform(get("/api/auth/me").cookie(cookie(r, "__Host-barizone_session")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value("admin"));
    }

    @Test
    void wrongPasswordAndMissingOriginAreRejected() throws Exception {
        mvc.perform(json(post("/api/auth/login"), "{\"password\":\"sbagliata\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.detail").value("Credenziali non valide."));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"password-di-servizio\"}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail").value("Richiesta non autorizzata."));
        mvc.perform(post("/api/auth/login").header("Origin", "https://attaccante.example")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"password-di-servizio\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void databaseAccountAcceptsHashFromWorkerVersion() throws Exception {
        jdbc.execute(ACCOUNT);
        MvcResult r = mvc.perform(json(post("/api/auth/login"), "{\"username\":\" Redazione \",\"password\":\"Password-Di-Prova!\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value("redazione")).andReturn();
        Cookie session = cookie(r, "__Host-barizone_session");
        assertThat(session.getValue()).matches("^[a-f0-9]{64}$");
        // nel DB c'è solo l'hash del token
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM admin_sessions WHERE token_hash = ?", Long.class, session.getValue())).isZero();

        mvc.perform(get("/api/auth/me").cookie(session)).andExpect(jsonPath("$.name").value("Redazione"));
        mvc.perform(post("/api/auth/logout").header("Origin", ORIGIN).cookie(session))
                .andExpect(status().isOk()).andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Max-Age=0")));
        mvc.perform(get("/api/auth/me").cookie(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void tenFailuresLockTheAccountForFifteenMinutes() throws Exception {
        jdbc.execute(ACCOUNT);
        for (int i = 0; i < 10; i++) {
            mvc.perform(json(post("/api/auth/login"), "{\"username\":\"redazione\",\"password\":\"x\"}"))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(json(post("/api/auth/login"), "{\"username\":\"redazione\",\"password\":\"Password-Di-Prova!\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.detail").value("Troppi tentativi. Riprova tra 15 minuti."));
    }

    @Test
    void reservedAreasRequireAdmin() throws Exception {
        mvc.perform(get("/api/admin/news")).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Accesso riservato all’amministratore."));
        mvc.perform(get("/api/admin/subscribers").cookie(new Cookie("__Host-barizone_session", "falso.token.x")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/admin")).andExpect(status().isFound())
                .andExpect(header().string("Location", "/login?next=%2Fadmin"));
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void metricsAndApiDocsAreAdminOnly() throws Exception {
        mvc.perform(get("/actuator/prometheus")).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/openapi")).andExpect(status().isForbidden());
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
        Cookie admin = adminCookie();
        mvc.perform(get("/actuator/prometheus").cookie(admin)).andExpect(status().isOk());
        mvc.perform(get("/api/admin/openapi").cookie(admin)).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/news']").exists());
    }
}
