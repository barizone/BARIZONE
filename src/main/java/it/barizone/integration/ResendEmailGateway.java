package it.barizone.integration;

import java.util.List;
import java.util.Map;

import it.barizone.configuration.BarizoneProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Resend, endpoint {@code POST /emails/batch} con chiave di idempotenza: un invio ripetuto
 * dello stesso modulo non genera email doppie. Documentazione:
 * https://resend.com/docs/api-reference/emails/send-batch-emails
 */
@Component
public class ResendEmailGateway implements EmailGateway {

    private static final Logger log = LoggerFactory.getLogger(ResendEmailGateway.class);
    private final RestClient client;
    private final BarizoneProperties.Email config;

    public ResendEmailGateway(RestClient.Builder builder, BarizoneProperties properties) {
        this.config = properties.email();
        this.client = builder.baseUrl(config.resendBaseUrl()).build();
    }

    @Override
    public boolean enabled() {
        return config.active();
    }

    @Override
    public String sendBatch(List<Email> emails, String idempotencyKey) {
        if (!enabled()) {
            return "disabled";
        }
        List<Map<String, Object>> payload = emails.stream().map(e -> Map.<String, Object>of(
                "from", e.from(), "to", e.to(), "reply_to", e.replyTo(), "subject", e.subject(), "text", e.text()))
                .toList();
        try {
            Map<?, ?> result = client.post().uri("/emails/batch")
                    .header("Authorization", "Bearer " + config.resendApiKey())
                    .header("Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(Map.class);
            boolean ok = result != null && result.get("data") instanceof List<?> data && data.size() == emails.size()
                    && data.stream().allMatch(i -> i instanceof Map<?, ?> m && m.get("id") instanceof String id && !id.isEmpty());
            return ok ? "accepted" : "failed";
        } catch (RestClientException e) {
            log.error("barizone-signup-email: provider non disponibile ({})", e.getClass().getSimpleName());
            return "failed";
        }
    }
}
