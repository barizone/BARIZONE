package it.barizone.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import it.barizone.support.IntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

class GroupIntegrationTest extends IntegrationTest {

    private static final String PASSWORD = "Password-Valida-1";

    @Test
    void merchantAccountMessagesRegisteredUserAcrossDevices() throws Exception {
        Cookie merchant = approvedMerchantCookie("mario@example.it", PASSWORD, "Forno Bari", vat("0123456789"));
        String shop = jdbc.queryForObject("SELECT id FROM company_profiles", String.class);
        Cookie user = userCookie("anna@example.it", PASSWORD);

        mvc.perform(get("/api/groups/me").cookie(merchant)).andExpect(jsonPath("$.user.role").value("merchant"))
                .andExpect(jsonPath("$.user.shop_id").value(shop));
        mvc.perform(get("/api/groups/me").cookie(user)).andExpect(jsonPath("$.user").isEmpty());

        // Nessun aderente: niente messaggio
        mvc.perform(json(post("/api/groups/shops/" + shop + "/messages"), "{\"title\":\"Ciao\",\"body\":\"Novità\"}").cookie(merchant))
                .andExpect(status().isBadRequest());

        // L'utente registrato aderisce e attiva le notifiche: l'adesione è legata all'account
        mvc.perform(json(post("/api/groups/subscribe"), "{\"alias\":\"Anna\",\"shop_id\":\"" + shop + "\",\"consent\":true}").cookie(user))
                .andExpect(status().isCreated());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM merchant_clients WHERE account_id IS NOT NULL", Long.class)).isEqualTo(1);
        mvc.perform(json(post("/api/groups/push/subscription"), "{\"marketing_consent\":true,\"subscription\":" + subscriptionJson("anna") + "}").cookie(user))
                .andExpect(status().isOk());

        mvc.perform(json(post("/api/groups/shops/" + shop + "/messages"), "{\"title\":\"Pane caldo\",\"body\":\"Sfornato ora\"}").cookie(merchant))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.recipients").value(1)).andExpect(jsonPath("$.accepted").value(1))
                .andExpect(jsonPath("$.more").value(false));
        assertThat(push.messages.getFirst().url()).isEqualTo("/adesioni?azienda=" + shop);

        // Un secondo accesso dello stesso utente (altro dispositivo, nessun cookie anonimo) vede le stesse adesioni
        MvcResult again = mvc.perform(json(post("/api/account/login"), "{\"email\":\"anna@example.it\",\"password\":\"" + PASSWORD + "\"}")).andReturn();
        mvc.perform(get("/api/groups/inbox").cookie(cookie(again, "__Host-barizone_account")))
                .andExpect(jsonPath("$.memberships[0].shop_name").value("Forno Bari"))
                .andExpect(jsonPath("$.messages", hasSize(1))).andExpect(jsonPath("$.push_active").value(true));

        mvc.perform(get("/api/groups/state").cookie(merchant)).andExpect(jsonPath("$.clients", hasSize(1)))
                .andExpect(jsonPath("$.messages[0].accepted").value(1));
        // L'utente non gestisce l'attività; nessun link di accesso per esercenti esiste più
        mvc.perform(json(post("/api/groups/shops/" + shop + "/messages"), "{\"title\":\"x\",\"body\":\"y\"}").cookie(user)).andExpect(status().isForbidden());
        mvc.perform(get("/api/groups/state").cookie(user)).andExpect(status().isUnauthorized());
        mvc.perform(json(post("/api/groups/shops/" + shop + "/access"), "{}").cookie(adminCookie())).andExpect(status().isNotFound());
    }

    @Test
    void merchantWithPendingRegistrationCannotManageGroups() throws Exception {
        mvc.perform(json(post("/api/account/register-business"), businessRegistrationJson("mario@example.it", PASSWORD, "Forno Bari", vat("0123456789"))));
        mvc.perform(json(post("/api/account/verify-email"), "{\"token\":\"" + lastEmailToken() + "\"}"));
        Cookie merchant = cookie(mvc.perform(json(post("/api/account/login"),
                "{\"email\":\"mario@example.it\",\"password\":\"" + PASSWORD + "\"}")).andReturn(), "__Host-barizone_account");
        mvc.perform(get("/api/groups/me").cookie(merchant)).andExpect(jsonPath("$.user").isEmpty());
        mvc.perform(get("/api/groups/state").cookie(merchant)).andExpect(status().isUnauthorized());
    }

    @Test
    void anonymousMembershipStillWorksAndMovesToTheAccountAtLogin() throws Exception {
        approvedMerchantCookie("mario@example.it", PASSWORD, "Forno Bari", vat("0123456789"));
        String shop = jdbc.queryForObject("SELECT id FROM company_profiles", String.class);

        MvcResult joined = mvc.perform(json(post("/api/groups/subscribe"), "{\"alias\":\"Ospite\",\"shop_id\":\"" + shop + "\",\"consent\":true}"))
                .andExpect(status().isCreated()).andReturn();
        Cookie anonymous = cookie(joined, "__Host-barizone_customer");
        mvc.perform(get("/api/groups/inbox").cookie(anonymous)).andExpect(jsonPath("$.memberships", hasSize(1)));

        // Se poi l'utente accede da quel dispositivo, l'adesione anonima passa al suo account
        Cookie user = userCookie("anna@example.it", PASSWORD);
        mvc.perform(get("/api/groups/inbox").cookie(user).cookie(anonymous)).andExpect(jsonPath("$.memberships", hasSize(1)));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM merchant_clients WHERE account_id IS NOT NULL", Long.class)).isEqualTo(1);
        mvc.perform(get("/api/groups/inbox").cookie(user)).andExpect(jsonPath("$.memberships", hasSize(1)));
    }
}
