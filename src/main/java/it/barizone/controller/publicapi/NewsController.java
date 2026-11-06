package it.barizone.controller.publicapi;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import it.barizone.dto.response.NewsResponse;
import it.barizone.service.NewsSelectionService;
import it.barizone.service.NewsService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/news")
public class NewsController {

    private final NewsService newsService;
    private final Map<String, Object> editorialStatus;

    public NewsController(NewsService newsService, ObjectMapper mapper) throws IOException {
        this.newsService = newsService;
        try (InputStream in = new ClassPathResource("editorial/status.json").getInputStream()) {
            this.editorialStatus = mapper.readValue(in, new TypeReference<>() { });
        }
    }

    @GetMapping
    public ResponseEntity<List<NewsResponse>> list(@RequestParam(defaultValue = "all") String scope,
                                                   @RequestParam(required = false) String neighborhood,
                                                   @RequestParam(required = false) String category,
                                                   @RequestParam(required = false) String limit,
                                                   @RequestParam(required = false) String offset) {
        return noStore(newsService.publicList(criteria(scope, neighborhood, category, false, limit, offset)));
    }

    @GetMapping("/featured")
    public ResponseEntity<List<NewsResponse>> featured(@RequestParam(defaultValue = "all") String scope,
                                                       @RequestParam(required = false) String neighborhood,
                                                       @RequestParam(required = false) String category,
                                                       @RequestParam(required = false) String limit,
                                                       @RequestParam(required = false) String offset) {
        return noStore(newsService.publicList(criteria(scope, neighborhood, category, true, limit, offset)));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        return noStore(editorialStatus);
    }

    @GetMapping("/{id}")
    public ResponseEntity<NewsResponse> detail(@PathVariable String id) {
        return noStore(newsService.publicDetail(id));
    }

    /** I commenti non sono attivi: l'endpoint resta per compatibilità con il frontend. */
    @GetMapping("/{id}/comments")
    public List<Object> comments(@PathVariable String id) {
        return List.of();
    }

    private static NewsSelectionService.Criteria criteria(String scope, String neighborhood, String category,
                                                          boolean featured, String limit, String offset) {
        int lim = Math.min(100, Math.max(1, parseOr(limit, 30)));
        int off = Math.max(0, parseOr(offset, 0));
        return new NewsSelectionService.Criteria(scope, neighborhood, category, featured, lim, off);
    }

    private static int parseOr(String value, int fallback) {
        try {
            int v = (int) Math.floor(Double.parseDouble(value));
            return v == 0 && fallback != 0 ? fallback : v;
        } catch (NumberFormatException | NullPointerException e) {
            return fallback;
        }
    }

    private static <T> ResponseEntity<T> noStore(T body) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
    }
}
