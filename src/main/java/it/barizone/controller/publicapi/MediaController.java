package it.barizone.controller.publicapi;

import java.io.IOException;
import java.util.Map;

import it.barizone.exception.ApiException;
import it.barizone.security.CurrentAdmin;
import it.barizone.service.MediaService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.TimeUnit;

@RestController
public class MediaController {

    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @PostMapping("/api/admin/media")
    public ResponseEntity<Map<String, String>> upload(@RequestParam(name = "photo", required = false) MultipartFile photo,
                                                      HttpServletRequest request) throws IOException {
        if (request.getContentLengthLong() > 1_100_000) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "Foto troppo grande: massimo 1 MB.");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("url", mediaService.upload(photo)));
    }

    @GetMapping("/api/media/{id}")
    public ResponseEntity<byte[]> get(@PathVariable String id) {
        boolean admin = CurrentAdmin.isAdmin();
        MediaService.Media media = mediaService.load(id, admin)
                .orElseThrow(() -> ApiException.notFound("Foto non trovata."));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(media.mime()))
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(admin ? CacheControl.noStore().cachePrivate() : CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                .body(media.content());
    }
}
