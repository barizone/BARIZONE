package it.barizone.model.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import it.barizone.model.enums.NewsOrigin;
import it.barizone.model.enums.NewsStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "news")
@Getter
@Setter
@NoArgsConstructor
public class NewsArticle {

    @Id
    private String id;
    private String title;
    private String subtitle = "";
    private String content;
    private String category;
    private String quartiere = "";

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]")
    private List<String> neighborhoods = new ArrayList<>();

    private String coverImage = "";
    private String coverCaption;
    private String imageSourceUrl;
    private String imageLicenseUrl;
    private String videoUrl = "";
    private String sourceUrl = "";
    private String author = "Redazione Barizone";

    @Enumerated(EnumType.STRING)
    private NewsStatus status = NewsStatus.draft;

    @Enumerated(EnumType.STRING)
    private NewsOrigin origin = NewsOrigin.admin;

    private boolean isFeatured;
    private int views;
    private String factDate;
    private LocalDate editionDate;
    private Instant expiresAt;
    private Instant publishedAt;
    private Instant updatedAt;

    /** Copia per l'archivio delle versioni: stessi contenuti, nuovo id, stato archiviato. */
    public NewsArticle archivedCopy(String newId, Instant now) {
        NewsArticle copy = new NewsArticle();
        copy.id = newId;
        copy.title = title;
        copy.subtitle = subtitle;
        copy.content = content;
        copy.category = category;
        copy.quartiere = quartiere;
        copy.neighborhoods = new ArrayList<>(neighborhoods);
        copy.coverImage = coverImage;
        copy.coverCaption = coverCaption;
        copy.imageSourceUrl = imageSourceUrl;
        copy.imageLicenseUrl = imageLicenseUrl;
        copy.videoUrl = videoUrl;
        copy.sourceUrl = sourceUrl;
        copy.author = author;
        copy.status = NewsStatus.archived;
        copy.origin = origin;
        copy.isFeatured = false;
        copy.factDate = factDate;
        copy.editionDate = editionDate;
        copy.expiresAt = expiresAt;
        copy.publishedAt = publishedAt;
        copy.updatedAt = now;
        return copy;
    }
}
