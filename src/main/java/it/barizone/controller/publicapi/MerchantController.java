package it.barizone.controller.publicapi;

import java.util.List;

import it.barizone.dto.response.MerchantResponse;
import it.barizone.dto.response.SpotlightResponse;
import it.barizone.service.MerchantService;
import it.barizone.service.SpotlightService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/merchants")
public class MerchantController {

    private final MerchantService merchantService;
    private final SpotlightService spotlightService;

    public MerchantController(MerchantService merchantService, SpotlightService spotlightService) {
        this.merchantService = merchantService;
        this.spotlightService = spotlightService;
    }

    @GetMapping
    public List<MerchantResponse> list(@RequestParam(required = false) String quartiere) {
        return merchantService.publicList(quartiere);
    }

    @GetMapping("/spotlight")
    public SpotlightResponse spotlight() {
        return spotlightService.today();
    }

    @GetMapping("/{id}")
    public MerchantResponse detail(@PathVariable String id) {
        return merchantService.publicDetail(id);
    }
}
