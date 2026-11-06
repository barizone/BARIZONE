package it.barizone.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import it.barizone.support.IntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;

class NewsIntegrationTest extends IntegrationTest {

    private static final String ARTICLE = """
            {"title":"Nuova pista ciclabile a Japigia","subtitle":"Lavori da lunedì","content":"Testo della notizia.",
             "category":"Attualità","quartiere":"Japigia","source_url":"https://www.comune.bari.it/x",
             "status":"published","is_featured":true,"published_at":"2026-11-06T09:00:00.000Z"}""";

    @Test
    void editorialArchiveMatchesWorkerSelection() throws Exception {
        seeder.run(null);
        // Valori ottenuti eseguendo editorialSelection.js originale sugli stessi dati e alla stessa data.
        mvc.perform(get("/api/news?scope=current&limit=100")).andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].id").value("bz-20261106-santo-spirito-acque"))
                .andExpect(jsonPath("$[0].body").exists());
        mvc.perform(get("/api/news?scope=archive&limit=100")).andExpect(jsonPath("$", hasSize(12)));
        mvc.perform(get("/api/news?scope=neighborhood&limit=100")).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/news?scope=current&neighborhood=Carrassi")).andExpect(jsonPath("$[0].id").value("bz-20261105-carrassi-natale"));
        mvc.perform(get("/api/news")).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/news/bz-20261019-acar")).andExpect(status().isOk());
        mvc.perform(get("/api/news/inesistente")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Notizia non trovata"));
    }

    @Test
    void adminPublishesAndCorrectionsKeepPreviousVersion() throws Exception {
        Cookie admin = adminCookie();
        String created = mvc.perform(json(post("/api/admin/news"), ARTICLE).cookie(admin))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.is_featured").value(true))
                .andExpect(jsonPath("$.is_demo").value(false))
                .andExpect(jsonPath("$.author").value("Redazione Barizone"))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(created, "$.id");

        mvc.perform(get("/api/news?scope=all")).andExpect(jsonPath("$[0].id").value(id));
        mvc.perform(get("/api/news/featured?scope=all")).andExpect(jsonPath("$", hasSize(1)));

        mvc.perform(json(put("/api/admin/news/" + id), ARTICLE.replace("Lavori da lunedì", "Lavori da martedì")).cookie(admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.subtitle").value("Lavori da martedì"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM news WHERE status = 'archived' AND subtitle = 'Lavori da lunedì'", Long.class))
                .isEqualTo(1);
        String archivedId = jdbc.queryForObject("SELECT id FROM news WHERE status = 'archived'", String.class);
        mvc.perform(json(put("/api/admin/news/" + archivedId), ARTICLE).cookie(admin))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value("La versione archiviata non è modificabile."));
    }

    @Test
    void validationMessagesAreInItalian() throws Exception {
        Cookie admin = adminCookie();
        mvc.perform(json(post("/api/admin/news"), ARTICLE.replace("\"Nuova pista ciclabile a Japigia\"", "\"\"")).cookie(admin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Inserisci un titolo (massimo 200 caratteri) e il testo (massimo 30.000 caratteri)."));
        mvc.perform(json(post("/api/admin/news"), ARTICLE.replace("Attualità", "Gossip")).cookie(admin))
                .andExpect(jsonPath("$.detail").value("Categoria o stato non valido."));
        mvc.perform(json(post("/api/admin/news"), ARTICLE.replace("https://www.comune.bari.it/x", "")).cookie(admin))
                .andExpect(jsonPath("$.detail").value("Per pubblicare serve il collegamento alla fonte originale."));
        mvc.perform(json(post("/api/admin/news"), ARTICLE.replace("https://www.comune.bari.it/x", "javascript:alert(1)")).cookie(admin))
                .andExpect(jsonPath("$.detail").value("Inserisci un collegamento completo e valido."));
        mvc.perform(json(post("/api/admin/news"), ARTICLE.replace("2026-11-06T09:00:00.000Z", "ieri")).cookie(admin))
                .andExpect(jsonPath("$.detail").value("Inserisci una data valida."));
    }

    @Test
    void seoListsPublicPages() throws Exception {
        seeder.run(null);
        String xml = mvc.perform(get("/sitemap.xml")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(xml).contains("<loc>http://localhost/notizie/bz-20261106-santo-spirito-acque</loc>", "<loc>http://localhost/aziende</loc>");
        String robots = mvc.perform(get("/robots.txt")).andReturn().getResponse().getContentAsString();
        assertThat(robots).contains("Disallow: /api/", "Sitemap: http://localhost/sitemap.xml");
    }
}
