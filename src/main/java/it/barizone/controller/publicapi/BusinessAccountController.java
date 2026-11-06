package it.barizone.controller.publicapi;

import it.barizone.dto.request.AccountRequests;
import it.barizone.dto.response.AccountResponse;
import it.barizone.dto.response.MerchantResponse;
import it.barizone.security.CurrentAccount;
import it.barizone.service.BusinessRegistrationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Area riservata all'attività (ruolo MERCHANT, vedi SecurityConfig): consultazione e modifica dei propri dati. */
@RestController
@RequestMapping("/api/account/business")
public class BusinessAccountController {

    private final BusinessRegistrationService businesses;

    public BusinessAccountController(BusinessRegistrationService businesses) {
        this.businesses = businesses;
    }

    @GetMapping
    public MerchantResponse get() {
        return MerchantResponse.of(businesses.own(CurrentAccount.get().orElseThrow().id()), java.util.List.of());
    }

    @PutMapping
    public AccountResponse.BusinessSummary update(@RequestBody AccountRequests.Business body) {
        return businesses.updateOwn(CurrentAccount.get().orElseThrow().id(), body);
    }
}
