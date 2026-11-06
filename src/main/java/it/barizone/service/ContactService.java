package it.barizone.service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import it.barizone.configuration.BarizoneProperties;
import it.barizone.dto.request.ContactRequest;
import it.barizone.dto.response.ContactResponse;
import it.barizone.exception.ApiException;
import it.barizone.model.entity.AppConfig;
import it.barizone.repository.AppConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/** Contatti pubblici: email della redazione e numero WhatsApp (visibile solo se pubblicato). */
@Service
public class ContactService {

    private static final String KEY = "barizone-contact";

    private final AppConfigRepository config;
    private final BarizoneProperties properties;
    private final ObjectMapper mapper;

    public ContactService(AppConfigRepository config, BarizoneProperties properties, ObjectMapper mapper) {
        this.config = config;
        this.properties = properties;
        this.mapper = mapper;
    }

    public static String whatsappLink(String number) {
        String digits = number == null ? "" : number.replaceAll("[\\s()+.-]", "");
        return digits.matches("^[1-9]\\d{7,14}$") ? "https://wa.me/" + digits : null;
    }

    @Transactional(readOnly = true)
    public ContactResponse get(boolean admin) {
        Map<?, ?> saved = config.findById(KEY).map(c -> mapper.readValue(c.getValue(), Map.class))
                .orElse(Map.of("whatsapp_number", "", "published", false));
        String number = saved.get("whatsapp_number") instanceof String s ? s : "";
        boolean enabled = Boolean.TRUE.equals(saved.get("published")) && whatsappLink(number) != null;
        boolean visible = admin || enabled;
        return new ContactResponse(properties.contactEmail(), properties.email().active(),
                visible ? number : "", visible ? whatsappLink(number) : null, enabled);
    }

    @Transactional
    public Map<String, Object> update(ContactRequest body) {
        if (!(body.whatsappNumber() instanceof String number) || number.length() > 30
                || !(body.published() instanceof Boolean published)) {
            throw ApiException.badRequest("Controlla numero e visibilità.");
        }
        String link = whatsappLink(number);
        if (!number.isBlank() && link == null) {
            throw ApiException.badRequest("Inserisci il numero completo di prefisso internazionale.");
        }
        if (published && link == null) {
            throw ApiException.badRequest("Inserisci un numero prima di pubblicarlo.");
        }
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("whatsapp_number", link != null ? number.trim() : "");
        value.put("published", published);
        value.put("updated_at", Instant.now().toString());
        config.save(new AppConfig(KEY, mapper.writeValueAsString(value)));
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("ok", true);
        response.putAll(value);
        response.put("whatsapp_url", link);
        return response;
    }
}
