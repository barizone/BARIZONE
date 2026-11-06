package it.barizone.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import it.barizone.support.IntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

class GroupIntegrationTest extends IntegrationTest {

    @Test
    void merchantAccessClientMembershipAndMessage() throws Exception {
        Cookie admin = adminCookie();
        String shop = JsonPath.read(mvc.perform(json(post("/api/admin/merchants"), "{\"business_name\":\"Forno Bari\"}").cookie(admin))
                .andReturn().getResponse().getContentAsString(), "$.id");
        mvc.perform(post("/api/admin/merchants/" + shop + "/status?status=published").header("Origin", ORIGIN).cookie(admin));

        // L'admin genera il link monouso
        String url = JsonPath.read(mvc.perform(json(post("/api/groups/shops/" + shop + "/access"), "{}").cookie(admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.expires_hours").value(12))
                .andReturn().getResponse().getContentAsString(), "$.url");
        String token = url.substring(url.indexOf("accesso=") + 8);

        MvcResult login = mvc.perform(json(post("/api/groups/merchant-login"), "{\"token\":\"" + token + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.shop_id").value(shop)).andReturn();
        Cookie merchant = cookie(login, "__Host-barizone_merchant");
        assertThat(login.getResponse().getHeader("Set-Cookie")).contains("SameSite=Strict");
        // Monouso
        mvc.perform(json(post("/api/groups/merchant-login"), "{\"token\":\"" + token + "\"}")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/groups/me").cookie(merchant)).andExpect(jsonPath("$.user.role").value("merchant"));

        // Nessun aderente: niente messaggio
        mvc.perform(json(post("/api/groups/shops/" + shop + "/messages"), "{\"title\":\"Ciao\",\"body\":\"Novità\"}").cookie(merchant))
                .andExpect(status().isBadRequest());

        // Un cliente aderisce e attiva le notifiche
        MvcResult joined = mvc.perform(json(post("/api/groups/subscribe"),
                        "{\"alias\":\"Anna\",\"shop_id\":\"" + shop + "\",\"consent\":true}"))
                .andExpect(status().isCreated()).andReturn();
        Cookie customer = cookie(joined, "__Host-barizone_customer");
        mvc.perform(json(post("/api/groups/push/subscription"), "{\"marketing_consent\":true,\"subscription\":"
                + subscriptionJson("anna") + "}").cookie(customer)).andExpect(status().isOk());

        mvc.perform(json(post("/api/groups/shops/" + shop + "/messages"), "{\"title\":\"Pane caldo\",\"body\":\"Sfornato ora\"}").cookie(merchant))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recipients").value(1))
                .andExpect(jsonPath("$.accepted").value(1))
                .andExpect(jsonPath("$.more").value(false));
        assertThat(push.messages.getFirst().url()).isEqualTo("/adesioni?azienda=" + shop);

        mvc.perform(get("/api/groups/inbox").cookie(customer))
                .andExpect(jsonPath("$.memberships[0].shop_name").value("Forno Bari"))
                .andExpect(jsonPath("$.messages", hasSize(1)))
                .andExpect(jsonPath("$.push_active").value(true));
        mvc.perform(get("/api/groups/state").cookie(merchant))
                .andExpect(jsonPath("$.clients", hasSize(1))).andExpect(jsonPath("$.messages[0].accepted").value(1));

        // Un esercente non può gestire altre attività né assegnare accessi
        mvc.perform(json(post("/api/groups/shops/" + shop + "/access"), "{}").cookie(merchant)).andExpect(status().isForbidden());
        mvc.perform(get("/api/groups/state")).andExpect(status().isUnauthorized());
    }
}
