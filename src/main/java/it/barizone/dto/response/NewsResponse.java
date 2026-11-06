package it.barizone.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.barizone.model.entity.NewsArticle;
import it.barizone.model.enums.NewsStatus;

/** Notizia nel formato JSON della versione precedente ({@code body} = {@code content}). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record NewsResponse(
        String id,
        String title,
        String subtitle,
        String content,
        String body,
        String category,
        String quartiere,
        List<String> neighborhoods,
        String coverImage,
        String coverCaption,
        String imageSourceUrl,
        String imageLicenseUrl,
        String videoUrl,
        String sourceUrl,
        String author,
        NewsStatus status,
        @JsonProperty("is_featured") boolean featured,
        int views,
        String factDate,
        LocalDate editionDate,
        Instant expiresAt,
        Instant publishedAt,
        Instant updatedAt) {

    public static NewsResponse from(NewsArticle n) {
        return new NewsResponse(n.getId(), n.getTitle(), n.getSubtitle(), n.getContent(), n.getContent(),
                n.getCategory(), n.getQuartiere(),
                n.getNeighborhoods() == null || n.getNeighborhoods().isEmpty() ? null : n.getNeighborhoods(),
                n.getCoverImage(), n.getCoverCaption(), n.getImageSourceUrl(), n.getImageLicenseUrl(),
                n.getVideoUrl(), n.getSourceUrl(), n.getAuthor(), n.getStatus(), n.isFeatured(),
                n.getViews(), n.getFactDate(), n.getEditionDate(), n.getExpiresAt(), n.getPublishedAt(),
                n.getUpdatedAt());
    }
}
