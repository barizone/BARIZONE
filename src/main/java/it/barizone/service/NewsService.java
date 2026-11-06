package it.barizone.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import it.barizone.dto.request.NewsRequest;
import it.barizone.dto.response.NewsResponse;
import it.barizone.exception.ApiException;
import it.barizone.model.entity.NewsArticle;
import it.barizone.model.enums.NewsCategory;
import it.barizone.model.enums.NewsStatus;
import it.barizone.repository.NewsRepository;
import it.barizone.util.Texts;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NewsService {

    private static final EnumSet<NewsStatus> PUBLIC = EnumSet.of(NewsStatus.published, NewsStatus.archived);

    private final NewsRepository repository;
    private final NewsSelectionService selection;
    private final Clock clock;

    public NewsService(NewsRepository repository, NewsSelectionService selection, Clock clock) {
        this.repository = repository;
        this.selection = selection;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<NewsResponse> publicList(NewsSelectionService.Criteria criteria) {
        List<NewsArticle> stored = repository.findByStatusInOrderByPublishedAtDesc(PUBLIC, Limit.of(1000));
        return selection.select(stored, criteria, clock.instant()).stream().map(NewsResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public NewsResponse publicDetail(String id) {
        return repository.findByIdAndStatusIn(id, PUBLIC).map(NewsResponse::from)
                .orElseThrow(() -> ApiException.notFound("Notizia non trovata"));
    }

    @Transactional(readOnly = true)
    public List<NewsResponse> adminList() {
        return repository.findAllByOrderByUpdatedAtDesc().stream().map(NewsResponse::from).toList();
    }

    @Transactional
    public NewsResponse create(NewsRequest request) {
        NewsArticle article = new NewsArticle();
        article.setId(UUID.randomUUID().toString());
        apply(article, validate(request), Instant.now());
        return NewsResponse.from(repository.save(article));
    }

    /**
     * Aggiorna una notizia. Se era pubblicata e cambia nei contenuti, la versione precedente
     * resta consultabile in archivio (trasparenza sulle correzioni).
     */
    @Transactional
    public NewsResponse update(String id, NewsRequest request) {
        Valid data = validate(request);
        NewsArticle previous = repository.findById(id).orElseThrow(() -> ApiException.notFound("Notizia non trovata"));
        if (previous.getStatus() == NewsStatus.archived) {
            throw ApiException.conflict("La versione archiviata non è modificabile.");
        }
        Instant now = Instant.now();
        boolean contentChanged = previous.getStatus() == NewsStatus.published && data.status() == NewsStatus.published
                && (!Objects.equals(previous.getTitle(), data.title())
                    || !Objects.equals(previous.getSubtitle(), data.subtitle())
                    || !Objects.equals(previous.getContent(), data.content())
                    || !Objects.equals(previous.getSourceUrl(), data.sourceUrl())
                    || !Objects.equals(previous.getVideoUrl(), data.videoUrl())
                    || !Objects.equals(previous.getCoverImage(), data.coverImage())
                    || !Objects.equals(previous.getCategory(), data.category())
                    || !Objects.equals(previous.getQuartiere(), data.quartiere()));
        if (contentChanged) {
            repository.save(previous.archivedCopy(UUID.randomUUID().toString(), now));
        }
        apply(previous, data, now);
        return NewsResponse.from(repository.save(previous));
    }

    private record Valid(String title, String subtitle, String content, String category, String quartiere,
                         String coverImage, String sourceUrl, String videoUrl, String author, NewsStatus status,
                         boolean featured, Instant publishedAt) {
    }

    private static void apply(NewsArticle a, Valid d, Instant now) {
        a.setTitle(d.title());
        a.setSubtitle(d.subtitle());
        a.setContent(d.content());
        a.setCategory(d.category());
        a.setQuartiere(d.quartiere());
        a.setCoverImage(d.coverImage());
        a.setSourceUrl(d.sourceUrl());
        a.setVideoUrl(d.videoUrl());
        a.setAuthor(d.author());
        a.setStatus(d.status());
        a.setFeatured(d.featured());
        a.setPublishedAt(d.publishedAt());
        a.setUpdatedAt(now);
    }

    private static Valid validate(NewsRequest r) {
        String title = Texts.clean(r.title());
        String content = Texts.clean(r.content());
        if (title.isEmpty() || title.length() > 200 || content.isEmpty() || content.length() > 30_000) {
            throw ApiException.badRequest("Inserisci un titolo (massimo 200 caratteri) e il testo (massimo 30.000 caratteri).");
        }
        String category = Texts.clean(r.category());
        String status = Texts.clean(r.status());
        if (!NewsCategory.isValid(category) || !(status.equals("draft") || status.equals("published"))) {
            throw ApiException.badRequest("Categoria o stato non valido.");
        }
        Instant publishedAt = parseDate(Texts.clean(r.publishedAt()));
        String sourceUrl = Texts.clean(r.sourceUrl());
        String coverImage = Texts.clean(r.coverImage());
        String videoUrl = Texts.clean(r.videoUrl());
        for (String url : List.of(sourceUrl, coverImage, videoUrl)) {
            if (!url.isEmpty() && Texts.parseHttpUrl(url) == null) {
                throw ApiException.badRequest("Inserisci un collegamento completo e valido.");
            }
        }
        NewsStatus newsStatus = NewsStatus.valueOf(status);
        if (newsStatus == NewsStatus.published && sourceUrl.isEmpty()) {
            throw ApiException.badRequest("Per pubblicare serve il collegamento alla fonte originale.");
        }
        String author = Texts.clean(r.author());
        return new Valid(title, Texts.clean(r.subtitle()), content, category, Texts.clean(r.quartiere()), coverImage,
                sourceUrl, videoUrl, author.isEmpty() ? "Redazione Barizone" : author, newsStatus,
                Boolean.TRUE.equals(r.isFeatured()), publishedAt);
    }

    /** Accetta ISO-8601 con o senza fuso (senza fuso = UTC, come la versione Worker) o la sola data. */
    static Instant parseDate(String value) {
        if (!value.isEmpty()) {
            try {
                return OffsetDateTime.parse(value).toInstant();
            } catch (DateTimeParseException ignored) {
                // prova i formati successivi
            }
            try {
                return LocalDateTime.parse(value).toInstant(ZoneOffset.UTC);
            } catch (DateTimeParseException ignored) {
                // prova la sola data
            }
            try {
                return LocalDate.parse(value).atStartOfDay().toInstant(ZoneOffset.UTC);
            } catch (DateTimeParseException ignored) {
                // data non valida
            }
        }
        throw ApiException.badRequest("Inserisci una data valida.");
    }
}
