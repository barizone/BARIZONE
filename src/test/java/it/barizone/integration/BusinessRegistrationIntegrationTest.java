package it.barizone.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import it.barizone.support.IntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

class BusinessRegistrationIntegrationTest extends IntegrationTest {

    private static final String PASSWORD = "Password-Valida-1";
    private final String vat = vat("0123456789");

    private Cookie login(String email) throws Exception {
        MvcResult r = mvc.perform(json(post("/api/account/login"), "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}")).andReturn();
        return cookie(r, "__Host-barizone_account");
    }

    private String companyId() {
        return jdbc.queryForObject("SELECT id FROM company_profiles WHERE vat_number = ?", String.class, vat);
    }

    @Test
    void registrationGoesThroughVerificationAndAdminApproval() throws Exception {
        mvc.perform(json(post("/api/account/register-business"), businessRegistrationJson("mario@example.it", PASSWORD, "Panificio Rossi", vat)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.verification_required").value(true));

        // Registrata ma non visibile: né in elenco né come scheda
        mvc.perform(get("/api/merchants")).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/merchants/" + companyId())).andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("SELECT province FROM company_profiles", String.class)).isEqualTo("BA");
        assertThat(jdbc.queryForObject("SELECT terms_version FROM company_profiles", String.class)).isEqualTo("accounts-v1");

        mvc.perform(json(post("/api/account/verify-email"), "{\"token\":\"" + lastEmailToken() + "\"}"))
                .andExpect(jsonPath("$.role").value("merchant"));
        Cookie merchant = login("mario@example.it");
        mvc.perform(get("/api/account/me").cookie(merchant)).andExpect(jsonPath("$.role").value("merchant"))
                .andExpect(jsonPath("$.business.registration_status").value("pending"))
                .andExpect(jsonPath("$.business.published").value(false));

        // L'admin vede la richiesta con i dati del titolare e la approva
        Cookie admin = adminCookie();
        mvc.perform(get("/api/admin/registrations").cookie(admin)).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].account_email").value("mario@example.it"))
                .andExpect(jsonPath("$[0].business.vat_number").value(vat))
                .andExpect(jsonPath("$[0].business.registration_status").value("pending"));
        email.reset();
        mvc.perform(post("/api/admin/registrations/" + companyId() + "/approve").header("Origin", ORIGIN).cookie(admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("published")).andExpect(jsonPath("$.verified").value(true));
        assertThat(email.batches.getFirst().getFirst().to()).containsExactly("mario@example.it");

        mvc.perform(get("/api/merchants")).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].business_name").value("Panificio Rossi"))
                .andExpect(jsonPath("$[0].registration_status").doesNotExist())
                .andExpect(jsonPath("$[0].review_note").doesNotExist());
        mvc.perform(get("/api/admin/registrations").cookie(admin)).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(post("/api/admin/registrations/" + companyId() + "/approve").header("Origin", ORIGIN).cookie(admin))
                .andExpect(status().isConflict());
    }

    @Test
    void mandatoryDataAreChecked() throws Exception {
        String base = businessRegistrationJson("mario@example.it", PASSWORD, "Panificio Rossi", vat);
        check(base.replace(vat + "\",\"tax_code", "01234567890\",\"tax_code"), "La partita IVA non è valida: controlla le cifre.");
        check(base.replace("\"vat_number\":\"" + vat + "\"", "\"vat_number\":\"123\""), "La partita IVA deve contenere 11 cifre.");
        check(base.replace("\"tax_code\":\"" + vat + "\"", "\"tax_code\":\"ABC\""), "Il codice fiscale non è valido: sono 11 cifre (società) oppure 16 caratteri (persona fisica).");
        check(base.replace("\"tax_code\":\"" + vat + "\"", "\"tax_code\":\"\""), "Inserisci il codice fiscale.");
        check(base.replace("\"pec\":\"info@pec.example.it\"", "\"pec\":\"\""), "Inserisci un indirizzo PEC valido.");
        check(base.replace("\"legal_name\":\"Panificio Rossi S.n.c.\"", "\"legal_name\":\"\""), "Inserisci la ragione sociale (massimo 200 caratteri).");
        check(base.replace("\"postal_code\":\"70121\"", "\"postal_code\":\"701\""), "Il CAP deve contenere 5 cifre.");
        check(base.replace("\"province\":\"ba\"", "\"province\":\"Bari\""), "La provincia si indica con la sigla di 2 lettere (es. BA).");
        check(base.replace("\"quartiere\":\"Murat\"", "\"quartiere\":\"Tutta Bari\""), "Scegli il quartiere in cui si trova l’attività.");
        check(base.replace("\"phone\":\"+39 080 1234567\"", "\"phone\":\"abc\""), "Inserisci un numero di telefono valido.");
        check(base.replace("\"description\":\"Panificio artigianale con forno a legna dal 1980.\"", "\"description\":\"Breve\""), "Descrivi l’attività in almeno 20 caratteri (massimo 3000).");
        check(base.replace("\"represents_business\":true", "\"represents_business\":false"),
                "Devi dichiarare di essere il titolare o il legale rappresentante dell’attività, o di essere autorizzato a registrarla.");
        check(base.replace("\"accept_terms\":true", "\"accept_terms\":false"), "Per registrarti devi accettare le condizioni e l’informativa sulla privacy.");
        check(base.replace("\"business\":{", "\"x\":{"), "Inserisci i dati dell’attività.");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_accounts", Long.class)).as("nessun account creato").isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM company_profiles", Long.class)).isZero();
    }

    private void check(String body, String detail) throws Exception {
        mvc.perform(json(post("/api/account/register-business"), body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value(detail));
    }

    @Test
    void sameVatCannotBeRegisteredTwiceUnlessRejected() throws Exception {
        mvc.perform(json(post("/api/account/register-business"), businessRegistrationJson("mario@example.it", PASSWORD, "Panificio Rossi", vat)))
                .andExpect(status().isCreated());
        mvc.perform(json(post("/api/account/register-business"), businessRegistrationJson("altro@example.it", PASSWORD, "Altro Nome", vat)))
                .andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_accounts", Long.class)).isEqualTo(1);

        mvc.perform(json(post("/api/account/verify-email"), "{\"token\":\"" + lastEmailToken() + "\"}"));
        mvc.perform(json(post("/api/admin/registrations/" + companyId() + "/reject"), "{\"note\":\"PEC non corrisponde alla partita IVA\"}").cookie(adminCookie()))
                .andExpect(status().isOk());
        mvc.perform(json(post("/api/account/register-business"), businessRegistrationJson("altro@example.it", PASSWORD, "Altro Nome", vat)))
                .andExpect(status().isCreated());
    }

    @Test
    void rejectionNeedsReasonAndOwnerCanFixAndResubmit() throws Exception {
        mvc.perform(json(post("/api/account/register-business"), businessRegistrationJson("mario@example.it", PASSWORD, "Panificio Rossi", vat)));
        mvc.perform(json(post("/api/account/verify-email"), "{\"token\":\"" + lastEmailToken() + "\"}"));
        Cookie admin = adminCookie();
        mvc.perform(json(post("/api/admin/registrations/" + companyId() + "/reject"), "{\"note\":\"\"}").cookie(admin))
                .andExpect(status().isBadRequest());
        email.reset();
        mvc.perform(json(post("/api/admin/registrations/" + companyId() + "/reject"), "{\"note\":\"Manca la sede legale corretta\"}").cookie(admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.registration_status").value("rejected"));
        assertThat(email.batches.getFirst().getFirst().text()).contains("Manca la sede legale corretta");
        mvc.perform(get("/api/merchants")).andExpect(jsonPath("$", hasSize(0)));

        Cookie merchant = login("mario@example.it");
        mvc.perform(get("/api/account/me").cookie(merchant)).andExpect(jsonPath("$.business.registration_status").value("rejected"))
                .andExpect(jsonPath("$.business.review_note").value("Manca la sede legale corretta"));
        mvc.perform(json(put("/api/account/business"), businessJson("Panificio Rossi", vat).replace("Via Sparano 10\",\"postal", "Via Argiro 5\",\"postal")).cookie(merchant))
                .andExpect(status().isOk()).andExpect(jsonPath("$.registration_status").value("pending"))
                .andExpect(jsonPath("$.review_note").value(""));
        mvc.perform(get("/api/admin/registrations").cookie(admin)).andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void changingIdentityDataAfterApprovalRequiresNewReview() throws Exception {
        Cookie merchant = approvedMerchantCookie("mario@example.it", PASSWORD, "Panificio Rossi", vat);
        mvc.perform(get("/api/merchants")).andExpect(jsonPath("$", hasSize(1)));

        // Modifica non sensibile: resta pubblicata
        mvc.perform(json(put("/api/account/business"), businessJson("Panificio Rossi", vat).replace("dal 1980.", "dal 1985, con consegna a domicilio.")).cookie(merchant))
                .andExpect(status().isOk()).andExpect(jsonPath("$.registration_status").value("approved")).andExpect(jsonPath("$.published").value(true));
        mvc.perform(get("/api/merchants")).andExpect(jsonPath("$[0].description").value("Panificio artigianale con forno a legna dal 1985, con consegna a domicilio."));

        // Cambio della ragione sociale: esce dal sito finché l'admin non la riapprova
        mvc.perform(json(put("/api/account/business"), businessJson("Panificio Rossi", vat).replace("Rossi S.n.c.", "Rossi S.r.l.")).cookie(merchant))
                .andExpect(jsonPath("$.registration_status").value("pending")).andExpect(jsonPath("$.published").value(false));
        mvc.perform(get("/api/merchants")).andExpect(jsonPath("$", hasSize(0)));

        // Non si può prendere la partita IVA di un'altra attività
        approvedMerchantCookie("altra@example.it", PASSWORD, "Altra Attività", vat("0123456780"));
        mvc.perform(json(put("/api/account/business"), businessJson("Panificio Rossi", vat("0123456780"))).cookie(merchant))
                .andExpect(status().isConflict());
    }
}
