package it.barizone.controller.publicapi;

import java.util.concurrent.TimeUnit;

import it.barizone.service.SeoService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SeoController {

    private final SeoService seoService;

    public SeoController(SeoService seoService) {
        this.seoService = seoService;
    }

    @GetMapping(value = "/robots.txt")
    public ResponseEntity<String> robots() {
        return ResponseEntity.ok().contentType(new MediaType("text", "plain", java.nio.charset.StandardCharsets.UTF_8))
                .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).cachePublic())
                .header("X-Content-Type-Options", "nosniff").body(seoService.robots());
    }

    @GetMapping(value = "/sitemap.xml")
    public ResponseEntity<String> sitemap() {
        return ResponseEntity.ok().contentType(new MediaType("application", "xml", java.nio.charset.StandardCharsets.UTF_8))
                .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).cachePublic())
                .header("X-Content-Type-Options", "nosniff").body(seoService.sitemap());
    }
}
