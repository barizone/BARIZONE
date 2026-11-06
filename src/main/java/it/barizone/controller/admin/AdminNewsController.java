package it.barizone.controller.admin;

import java.util.List;

import it.barizone.dto.request.NewsRequest;
import it.barizone.dto.response.NewsResponse;
import it.barizone.service.NewsService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Gestione delle notizie (solo admin: vedi SecurityConfig). */
@RestController
@RequestMapping("/api/admin/news")
public class AdminNewsController {

    private final NewsService newsService;

    public AdminNewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    @GetMapping
    public List<NewsResponse> list() {
        return newsService.adminList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NewsResponse create(@RequestBody NewsRequest request) {
        return newsService.create(request);
    }

    @PutMapping("/{id}")
    public NewsResponse update(@PathVariable String id, @RequestBody NewsRequest request) {
        return newsService.update(id, request);
    }
}
