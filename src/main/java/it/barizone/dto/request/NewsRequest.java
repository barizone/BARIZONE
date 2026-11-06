package it.barizone.dto.request;

/** Notizia inviata dall'admin. La validazione è in {@code NewsService}, con i messaggi della versione precedente. */
public record NewsRequest(
        String title,
        String subtitle,
        String content,
        String category,
        String quartiere,
        String coverImage,
        String sourceUrl,
        String videoUrl,
        String author,
        String status,
        Boolean isFeatured,
        String publishedAt) {
}
