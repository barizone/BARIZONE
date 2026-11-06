package it.barizone.service;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import it.barizone.exception.ApiException;
import it.barizone.integration.MediaStorage;
import it.barizone.model.entity.MediaFile;
import it.barizone.repository.CompanyProfileRepository;
import it.barizone.repository.MediaFileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Foto delle aziende: JPG, PNG o WebP fino a 1 MB, con verifica dei magic bytes. */
@Service
public class MediaService {

    public static final int MAX_BYTES = 1_048_576;
    private static final Set<String> TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final MediaFileRepository repository;
    private final CompanyProfileRepository companies;
    private final MediaStorage storage;

    public MediaService(MediaFileRepository repository, CompanyProfileRepository companies, MediaStorage storage) {
        this.repository = repository;
        this.companies = companies;
        this.storage = storage;
    }

    public record Media(byte[] content, String mime) {
    }

    @Transactional
    public String upload(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty() || file.getSize() > MAX_BYTES || !TYPES.contains(file.getContentType())) {
            throw ApiException.badRequest("Carica una foto JPG, PNG o WebP fino a 1 MB.");
        }
        byte[] bytes = file.getBytes();
        if (!matchesSignature(file.getContentType(), bytes)) {
            throw ApiException.badRequest("Il file non corrisponde al formato dichiarato.");
        }
        MediaFile media = new MediaFile();
        media.setId(UUID.randomUUID());
        media.setMime(file.getContentType());
        media.setSizeBytes(bytes.length);
        media.setCreatedAt(Instant.now());
        storage.store(media, bytes);
        repository.save(media);
        return "/api/media/" + media.getId();
    }

    /** Il pubblico vede solo le foto collegate ad aziende pubblicate; l'admin vede tutto. */
    @Transactional(readOnly = true)
    public Optional<Media> load(String id, boolean admin) {
        if (!id.matches("^[a-f0-9-]{36}$")) {
            throw ApiException.notFound("Foto non trovata.");
        }
        if (!admin && !companies.isMediaPublished("/api/media/" + id)) {
            throw ApiException.notFound("Foto non disponibile.");
        }
        UUID uuid;
        try {
            uuid = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw ApiException.notFound("Foto non trovata.");
        }
        return repository.findById(uuid).map(m -> new Media(storage.load(m), m.getMime()));
    }

    static boolean matchesSignature(String type, byte[] b) {
        return switch (type) {
            case "image/jpeg" -> b.length > 2 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8;
            case "image/png" -> b.length > 4 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G';
            case "image/webp" -> b.length > 12 && new String(b, 0, 4).equals("RIFF") && new String(b, 8, 4).equals("WEBP");
            default -> false;
        };
    }
}
