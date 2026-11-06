package it.barizone.controller.publicapi;

import java.util.Map;

import it.barizone.dto.request.ContactRequest;
import it.barizone.dto.response.ContactResponse;
import it.barizone.service.ContactService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @GetMapping("/api/contacts")
    public ContactResponse publicContacts() {
        return contactService.get(false);
    }

    @GetMapping("/api/admin/contacts")
    public ContactResponse adminContacts() {
        return contactService.get(true);
    }

    @PutMapping("/api/admin/contacts")
    public Map<String, Object> update(@RequestBody ContactRequest body) {
        return contactService.update(body);
    }
}
