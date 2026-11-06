package it.barizone.controller.publicapi;

import java.util.List;
import java.util.Map;

import it.barizone.dto.request.SubscribeRequest;
import it.barizone.dto.request.TokenRequest;
import it.barizone.dto.response.SubscriberAdminResponse;
import it.barizone.service.SubscriberService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SubscriberController {

    private final SubscriberService subscriberService;

    public SubscriberController(SubscriberService subscriberService) {
        this.subscriberService = subscriberService;
    }

    @GetMapping("/api/subscribers/config")
    public Map<String, Object> config() {
        return subscriberService.config();
    }

    @PostMapping("/api/subscribers")
    public ResponseEntity<Map<String, Object>> subscribe(@RequestBody SubscribeRequest body) {
        SubscriberService.SubscribeResult result = subscriberService.subscribe(body);
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(result.body());
    }

    @PostMapping("/api/subscribers/unsubscribe")
    public Map<String, Boolean> unsubscribe(@RequestBody TokenRequest body) {
        subscriberService.unsubscribe(body.token());
        return Map.of("ok", true);
    }

    @GetMapping("/api/admin/subscribers")
    public List<SubscriberAdminResponse> adminList() {
        return subscriberService.adminList();
    }
}
