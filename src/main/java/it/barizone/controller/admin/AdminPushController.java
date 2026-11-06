package it.barizone.controller.admin;

import java.util.Map;
import java.util.UUID;

import it.barizone.dto.request.CampaignRequest;
import it.barizone.dto.request.ConfirmRequest;
import it.barizone.dto.request.EndpointRequest;
import it.barizone.dto.response.SendResult;
import it.barizone.security.AdminPrincipal;
import it.barizone.security.CurrentAdmin;
import it.barizone.service.PushCampaignService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/admin/push")
public class AdminPushController {

    private final PushCampaignService campaignService;

    public AdminPushController(PushCampaignService campaignService) {
        this.campaignService = campaignService;
    }

    @GetMapping
    public Map<String, Object> overview() {
        return campaignService.overview();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, UUID> create(@RequestBody CampaignRequest body, HttpServletRequest request) {
        String createdBy = CurrentAdmin.get().map(AdminPrincipal::id).orElse("admin");
        return Map.of("id", campaignService.create(body, origin(request), createdBy));
    }

    @PutMapping("/{id}")
    public Map<String, UUID> update(@PathVariable UUID id, @RequestBody CampaignRequest body, HttpServletRequest request) {
        return Map.of("id", campaignService.update(id, body, origin(request)));
    }

    @PostMapping("/test")
    public Map<String, Integer> test(@RequestBody EndpointRequest body) {
        return campaignService.test(body);
    }

    @PostMapping("/{id}/send")
    public SendResult send(@PathVariable UUID id, @RequestBody ConfirmRequest body) {
        return campaignService.send(id, body);
    }

    private static String origin(HttpServletRequest request) {
        return ServletUriComponentsBuilder.fromContextPath(request).replacePath(null).build().toUriString();
    }
}
