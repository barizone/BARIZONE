package it.barizone.controller.admin;

import java.util.List;
import java.util.Map;

import it.barizone.dto.request.CompanyRequest;
import it.barizone.dto.response.MerchantResponse;
import it.barizone.service.MerchantService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/merchants")
public class AdminMerchantController {

    private final MerchantService merchantService;

    public AdminMerchantController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    @GetMapping
    public List<MerchantResponse> list() {
        return merchantService.adminList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MerchantResponse create(@RequestBody CompanyRequest request) {
        return merchantService.create(request);
    }

    @PutMapping("/{id}")
    public MerchantResponse update(@PathVariable String id, @RequestBody CompanyRequest request) {
        return merchantService.update(id, request);
    }

    @PostMapping("/{id}/status")
    public Map<String, Boolean> status(@PathVariable String id, @RequestParam(required = false) String status) {
        merchantService.changeStatus(id, status);
        return Map.of("ok", true);
    }
}
