package it.barizone.service;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import it.barizone.model.entity.NewsArticle;
import it.barizone.model.enums.NewsOrigin;
import it.barizone.model.enums.NewsStatus;
import it.barizone.repository.NewsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Importa all'avvio le notizie redazionali (prima incluse nel codice del Worker, {@code editorial.js}).
 * Inserisce solo quelle mancanti: le modifiche fatte dall'admin non vengono mai sovrascritte.
 */
@Service
public class EditorialSeedService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(EditorialSeedService.class);
    private final NewsRepository repository;
    private final ObjectMapper mapper;

    public EditorialSeedService(NewsRepository repository, ObjectMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws IOException {
        ClassPathResource resource = new ClassPathResource("editorial/editorial.json");
        if (!resource.exists()) {
            return;
        }
        List<Map<String, Object>> items;
        try (InputStream in = resource.getInputStream()) {
            items = mapper.readValue(in, new TypeReference<>() { });
        }
        int inserted = 0;
        for (Map<String, Object> item : items) {
            String id = str(item, "id");
            if (id == null || Boolean.TRUE.equals(item.get("is_demo")) || repository.existsById(id)) {
                continue;
            }
            repository.save(toArticle(item));
            inserted++;
        }
        if (inserted > 0) {
            log.info("Notizie redazionali importate: {}", inserted);
        }
    }

    @SuppressWarnings("unchecked")
    private static NewsArticle toArticle(Map<String, Object> m) {
        NewsArticle a = new NewsArticle();
        a.setId(str(m, "id"));
        a.setTitle(str(m, "title"));
        a.setSubtitle(orEmpty(str(m, "subtitle")));
        a.setContent(orEmpty(str(m, "content") != null ? str(m, "content") : str(m, "body")));
        a.setCategory(str(m, "category"));
        a.setQuartiere(orEmpty(str(m, "quartiere")));
        if (m.get("neighborhoods") instanceof List<?> list) {
            a.setNeighborhoods(((List<Object>) list).stream().map(String::valueOf).toList());
        }
        a.setCoverImage(orEmpty(str(m, "cover_image")));
        a.setCoverCaption(str(m, "cover_caption"));
        a.setImageSourceUrl(str(m, "image_source_url"));
        a.setImageLicenseUrl(str(m, "image_license_url"));
        a.setVideoUrl(orEmpty(str(m, "video_url")));
        a.setSourceUrl(orEmpty(str(m, "source_url")));
        a.setAuthor(str(m, "author") != null ? str(m, "author") : "Redazione Barizone");
        a.setStatus(NewsStatus.published);
        a.setOrigin(NewsOrigin.editorial);
        a.setFeatured(Boolean.TRUE.equals(m.get("is_featured")));
        a.setViews(m.get("views") instanceof Number n ? n.intValue() : 0);
        a.setFactDate(str(m, "fact_date"));
        a.setEditionDate(str(m, "edition_date") != null ? LocalDate.parse(str(m, "edition_date")) : null);
        a.setExpiresAt(instant(str(m, "expires_at")));
        Instant updated = instant(str(m, "updated_at"));
        Instant published = instant(str(m, "published_at"));
        // Senza data di pubblicazione la versione Worker usava updated_at per data e ordinamento.
        a.setPublishedAt(published != null ? published : updated);
        a.setUpdatedAt(updated != null ? updated : published);
        return a;
    }

    private static String str(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v == null ? null : String.valueOf(v);
    }

    private static Instant instant(String value) {
        return value == null ? null : OffsetDateTime.parse(value).toInstant();
    }

    private static String orEmpty(String s) {
        return s == null ? "" : s;
    }
}
