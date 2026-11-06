package it.barizone.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import it.barizone.model.entity.NewsArticle;
import it.barizone.model.enums.NewsOrigin;
import it.barizone.model.enums.NewsStatus;
import it.barizone.util.Texts;
import org.springframework.stereotype.Component;

/**
 * Selezione delle notizie per Home, quartieri e archivio. Porta 1:1 di {@code editorialSelection.js}.
 *
 * <ul>
 *   <li><b>edizione</b>: la data più recente tra le notizie in corso (edition_date o data di pubblicazione a Bari);</li>
 *   <li><b>current</b>: notizie dell'edizione corrente o eventi ancora in corso;</li>
 *   <li><b>archive</b>: notizie archiviate, scadute o di edizioni precedenti;</li>
 *   <li><b>neighborhood</b>: solo l'edizione corrente;</li>
 *   <li><b>all</b>: tutte le notizie non archiviate né scadute, senza l'archivio redazionale.</li>
 * </ul>
 * Dopo 72 ore dall'ultima edizione l'ordine ruota una volta al giorno, senza cambiare le date.
 */
@Component
public class NewsSelectionService {

    public record Criteria(String scope, String neighborhood, String category, boolean featured, int limit, int offset) {
    }

    public List<NewsArticle> select(List<NewsArticle> stored, Criteria c, Instant now) {
        boolean includeEditorial = !"all".equals(c.scope());
        // Le notizie dell'admin precedono quelle redazionali: a parità di fonte vince quella dell'admin.
        List<NewsArticle> combined = new ArrayList<>();
        stored.stream().filter(n -> n.getOrigin() == NewsOrigin.admin).forEach(combined::add);
        if (includeEditorial) {
            stored.stream().filter(n -> n.getOrigin() == NewsOrigin.editorial)
                    .filter(n -> n.getPublishedAt() == null || !n.getPublishedAt().isAfter(now))
                    .forEach(combined::add);
        }
        combined.removeIf(n -> n.getSourceUrl() == null || n.getSourceUrl().isBlank());

        List<NewsArticle> unique = new ArrayList<>();
        for (NewsArticle n : combined) {
            boolean duplicate = unique.stream().anyMatch(x -> x.getId().equals(n.getId())
                    || (n.getStatus() != NewsStatus.archived && x.getStatus() != NewsStatus.archived
                        && Objects.equals(x.getSourceUrl(), n.getSourceUrl())));
            if (!duplicate) {
                unique.add(n);
            }
        }

        String hood = c.neighborhood();
        List<NewsArticle> territorial = unique.stream()
                .filter(n -> hood == null || hood.isEmpty() || hood.equals(n.getQuartiere())
                        || (n.getNeighborhoods() != null && n.getNeighborhoods().contains(hood)))
                .toList();

        LocalDate edition = territorial.stream()
                .filter(n -> n.getStatus() != NewsStatus.archived && !expired(n, now))
                .map(NewsSelectionService::dateOf)
                .max(Comparator.naturalOrder()).orElse(null);

        List<NewsArticle> selected = new ArrayList<>(territorial.stream().filter(n -> {
            boolean expired = expired(n, now);
            boolean ongoing = n.getExpiresAt() != null && n.getExpiresAt().isAfter(now);
            boolean previous = n.getStatus() == NewsStatus.archived || expired
                    || (!ongoing && edition != null && dateOf(n).isBefore(edition));
            boolean inScope = switch (c.scope()) {
                case "neighborhood" -> n.getStatus() != NewsStatus.archived && !expired && dateOf(n).equals(edition);
                case "archive" -> previous;
                case "all" -> n.getStatus() != NewsStatus.archived && !expired;
                default -> !previous;
            };
            return inScope && (c.category() == null || c.category().isEmpty() || c.category().equals(n.getCategory()))
                    && (!c.featured() || n.isFeatured());
        }).sorted(Comparator.comparing(NewsSelectionService::sortInstant).reversed()).toList());

        if (("current".equals(c.scope()) || "neighborhood".equals(c.scope())) && edition != null && selected.size() > 1) {
            Instant editionEnd = OffsetDateTime.of(edition.atTime(23, 59, 59), ZoneOffset.ofHours(1)).toInstant();
            if (Duration.between(editionEnd, now).compareTo(Duration.ofDays(3)) >= 0) {
                LocalDate today = Texts.romeDate(now);
                long daysSinceEpoch = today.toEpochDay();
                Collections.rotate(selected, -(int) (daysSinceEpoch % selected.size()));
            }
        }
        int from = Math.min(c.offset(), selected.size());
        int to = Math.min(from + c.limit(), selected.size());
        return selected.subList(from, to);
    }

    private static boolean expired(NewsArticle n, Instant now) {
        return n.getExpiresAt() != null && !n.getExpiresAt().isAfter(now);
    }

    static LocalDate dateOf(NewsArticle n) {
        if (n.getEditionDate() != null) {
            return n.getEditionDate();
        }
        return Texts.romeDate(sortInstant(n));
    }

    private static Instant sortInstant(NewsArticle n) {
        return n.getPublishedAt() != null ? n.getPublishedAt()
                : n.getUpdatedAt() != null ? n.getUpdatedAt() : Instant.EPOCH;
    }
}
