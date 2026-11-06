package it.barizone.controller.admin;

import java.util.List;
import java.util.Map;

import it.barizone.dto.response.MerchantResponse;
import it.barizone.exception.ApiException;
import it.barizone.model.enums.RegistrationStatus;
import it.barizone.service.BusinessRegistrationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Verifica delle registrazioni delle attività: l'admin controlla i dati e approva o rifiuta. */
@RestController
@RequestMapping("/api/admin/registrations")
public class AdminRegistrationController {

    private final BusinessRegistrationService businesses;

    public AdminRegistrationController(BusinessRegistrationService businesses) {
        this.businesses = businesses;
    }

    public record Item(MerchantResponse business, String accountEmail, String accountName) {
    }

    @GetMapping
    public List<Item> list(@RequestParam(defaultValue = "pending") String status) {
        RegistrationStatus s;
        try {
            s = RegistrationStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("Stato non valido.");
        }
        return businesses.registrations(s).stream()
                .map(p -> new Item(MerchantResponse.of(p.company(), List.of()), p.accountEmail(), p.accountName())).toList();
    }

    @PostMapping("/{id}/approve")
    public MerchantResponse approve(@PathVariable String id) {
        return MerchantResponse.of(businesses.approve(id), List.of());
    }

    @PostMapping("/{id}/reject")
    public MerchantResponse reject(@PathVariable String id, @RequestBody Map<String, Object> body) {
        return MerchantResponse.of(businesses.reject(id, String.valueOf(body.getOrDefault("note", ""))), List.of());
    }
}
