package it.barizone.controller.publicapi;

import java.util.Map;

import it.barizone.dto.request.EndpointRequest;
import it.barizone.dto.request.PushSubscribeRequest;
import it.barizone.service.PushSubscriptionService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/push")
public class PushController {

    private final PushSubscriptionService pushService;

    public PushController(PushSubscriptionService pushService) {
        this.pushService = pushService;
    }

    @GetMapping("/key")
    public Map<String, String> key() {
        return Map.of("publicKey", pushService.publicKey());
    }

    @PostMapping("/subscription")
    public Map<String, Boolean> subscribe(@RequestBody PushSubscribeRequest body) {
        pushService.subscribe(body);
        return Map.of("saved", true);
    }

    @DeleteMapping("/subscription")
    public Map<String, Boolean> revoke(@RequestBody EndpointRequest body) {
        pushService.revoke(body);
        return Map.of("revoked", true);
    }

    @PostMapping("/status")
    public Map<String, Object> status(@RequestBody EndpointRequest body) {
        return pushService.status(body);
    }
}
