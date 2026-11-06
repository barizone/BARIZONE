package it.barizone.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import it.barizone.support.IntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class MerchantIntegrationTest extends IntegrationTest {

    private int sequence;

    private String createPublished(Cookie admin, String name, String quartiere) throws Exception {
        String body = "{\"business_name\":\"" + name + "\",\"quartiere\":\"" + quartiere + "\",\"vat_number\":\"" + vat("012345678" + sequence++) + "\","
                + "\"email\":\"info@negozio.it\",\"postal_code\":\"70121\",\"maps_url\":\"https://maps.app.goo.gl/abc\"}";
        String id = JsonPath.read(mvc.perform(json(post("/api/admin/merchants"), body).cookie(admin))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("draft"))
                .andReturn().getResponse().getContentAsString(), "$.id");
        mvc.perform(post("/api/admin/merchants/" + id + "/status?status=published").header("Origin", ORIGIN).cookie(admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ok").value(true));
        return id;
    }

    @Test
    void publishedCompaniesAreVisibleAndFilteredByDistrict() throws Exception {
        Cookie admin = adminCookie();
        String id = createPublished(admin, "Panificio Rossi", "bari vecchia");
        createPublished(admin, "Libreria Murat", "Murat");
        mvc.perform(json(post("/api/admin/merchants"), "{\"business_name\":\"Bozza\"}").cookie(admin));

        mvc.perform(get("/api/merchants")).andExpect(jsonPath("$", hasSize(2)));
        mvc.perform(get("/api/merchants?quartiere=San Nicola / Bari Vecchia")).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].quartiere").value("San Nicola / Bari Vecchia"))
                .andExpect(jsonPath("$[0].publication_issues").doesNotExist());
        mvc.perform(get("/api/merchants/" + id)).andExpect(jsonPath("$.business_name").value("Panificio Rossi"));
        mvc.perform(get("/api/admin/merchants").cookie(admin)).andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].publication_issues").isArray());
    }

    @Test
    void sameVatInTwoCompaniesIsRefusedWithAClearMessage() throws Exception {
        Cookie admin = adminCookie();
        String body = "{\"business_name\":\"Uno\",\"vat_number\":\"" + vat("0123456789") + "\"}";
        mvc.perform(json(post("/api/admin/merchants"), body).cookie(admin)).andExpect(status().isCreated());
        mvc.perform(json(post("/api/admin/merchants"), body.replace("Uno", "Due")).cookie(admin))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value("Esiste già un’azienda con questa partita IVA."));
    }

    @Test
    void validationAndSuspension() throws Exception {
        Cookie admin = adminCookie();
        mvc.perform(json(post("/api/admin/merchants"), "{\"business_name\":\"X\",\"vat_number\":\"123\"}").cookie(admin))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value("La partita IVA deve contenere 11 cifre."));
        mvc.perform(json(post("/api/admin/merchants"), "{\"business_name\":\"X\",\"vat_number\":\"01234567890\"}").cookie(admin))
                .andExpect(jsonPath("$.detail").value("La partita IVA non è valida: controlla le cifre."));
        mvc.perform(json(post("/api/admin/merchants"), "{\"business_name\":\"X\",\"maps_url\":\"https://evil.example/maps\"}").cookie(admin))
                .andExpect(jsonPath("$.detail").value("Inserisci un collegamento Google Maps valido."));
        mvc.perform(json(post("/api/admin/merchants"), "{\"business_name\":\"\"}").cookie(admin))
                .andExpect(jsonPath("$.detail").value("Inserisci il nome dell’attività (massimo 160 caratteri)."));

        String id = createPublished(admin, "Bar Centrale", "Murat");
        mvc.perform(json(put("/api/admin/merchants/" + id), "{\"business_name\":\"Bar Centrale 2\"}").cookie(admin))
                .andExpect(jsonPath("$.status").value("published"));
        mvc.perform(post("/api/admin/merchants/" + id + "/status?status=suspended").header("Origin", ORIGIN).cookie(admin));
        mvc.perform(get("/api/merchants/" + id)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Attività non disponibile."));
        mvc.perform(post("/api/admin/merchants/" + id + "/status?status=boh").header("Origin", ORIGIN).cookie(admin))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value("Stato non valido."));
    }

    @Test
    void spotlightIsStableWithinTheDay() throws Exception {
        Cookie admin = adminCookie();
        mvc.perform(get("/api/merchants/spotlight")).andExpect(jsonPath("$.total").value(0)).andExpect(jsonPath("$.merchant").isEmpty());
        createPublished(admin, "Uno", "Murat");
        createPublished(admin, "Due", "Murat");
        createPublished(admin, "Tre", "Murat");
        String first = JsonPath.read(mvc.perform(get("/api/merchants/spotlight")).andExpect(jsonPath("$.date").value("2026-11-06"))
                .andExpect(jsonPath("$.total").value(3)).andReturn().getResponse().getContentAsString(), "$.merchant.id");
        for (int i = 0; i < 3; i++) {
            mvc.perform(get("/api/merchants/spotlight")).andExpect(jsonPath("$.merchant.id").value(first));
        }
        assertThat(jdbc.queryForObject("SELECT merchant_id FROM business_spotlight WHERE spotlight_date = '2026-11-06'", String.class))
                .isEqualTo(first);
    }

    @Test
    void mediaUploadChecksSignatureAndVisibility() throws Exception {
        Cookie admin = adminCookie();
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 0, 0};
        MockMultipartFile fake = new MockMultipartFile("photo", "x.png", "image/png", "non-png".getBytes());
        mvc.perform(multipart("/api/admin/media").file(fake).header("Origin", ORIGIN).cookie(admin))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value("Il file non corrisponde al formato dichiarato."));

        String url = JsonPath.read(mvc.perform(multipart("/api/admin/media")
                        .file(new MockMultipartFile("photo", "x.png", "image/png", png)).header("Origin", ORIGIN).cookie(admin))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.url");
        assertThat(url).matches("^/api/media/[a-f0-9-]{36}$");

        // Non collegata ad aziende pubblicate: invisibile al pubblico, visibile all'admin.
        mvc.perform(get(url)).andExpect(status().isNotFound());
        mvc.perform(get(url).cookie(admin)).andExpect(status().isOk()).andExpect(content().bytes(png));

        String id = createPublished(admin, "Con foto", "Murat");
        mvc.perform(json(put("/api/admin/merchants/" + id), "{\"business_name\":\"Con foto\",\"cover_image\":\"" + url + "\"}").cookie(admin));
        mvc.perform(get(url)).andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("max-age=3600")));
    }

    @Test
    void contactsPublishWhatsappOnlyWhenValid() throws Exception {
        Cookie admin = adminCookie();
        mvc.perform(json(put("/api/admin/contacts"), "{\"whatsapp_number\":\"123\",\"published\":true}").cookie(admin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Inserisci il numero completo di prefisso internazionale."));
        mvc.perform(json(put("/api/admin/contacts"), "{\"whatsapp_number\":\"+39 333 123 4567\",\"published\":false}").cookie(admin))
                .andExpect(jsonPath("$.whatsapp_url").value("https://wa.me/393331234567"));
        mvc.perform(get("/api/contacts")).andExpect(jsonPath("$.whatsapp_url").isEmpty()).andExpect(jsonPath("$.published").value(false));
        mvc.perform(json(put("/api/admin/contacts"), "{\"whatsapp_number\":\"+39 333 123 4567\",\"published\":true}").cookie(admin));
        mvc.perform(get("/api/contacts")).andExpect(jsonPath("$.whatsapp_url").value("https://wa.me/393331234567"))
                .andExpect(jsonPath("$.email").value("info@barizone.it"));
    }
}
