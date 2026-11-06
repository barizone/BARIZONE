package it.barizone.service;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import it.barizone.configuration.BarizoneProperties;
import it.barizone.dto.request.SubscribeRequest;
import it.barizone.dto.response.SubscriberAdminResponse;
import it.barizone.exception.ApiException;
import it.barizone.integration.EmailGateway;
import it.barizone.model.entity.NewsSubscriber;
import it.barizone.model.entity.SubscriberConsent;
import it.barizone.model.enums.District;
import it.barizone.model.enums.SubscriberStatus;
import it.barizone.repository.NewsSubscriberRepository;
import it.barizone.repository.PushSubscriptionRepository;
import it.barizone.repository.SubscriberConsentRepository;
import it.barizone.util.Texts;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Iscrizioni alle news. L'iscrizione resta salvata anche se l'invio email fallisce;
 * il consenso è versionato e datato.
 */
@Service
public class SubscriberService {

    public static final String CONSENT_VERSION = "barizone-updates-v1";

    private final NewsSubscriberRepository subscribers;
    private final SubscriberConsentRepository consents;
    private final PushSubscriptionRepository pushSubscriptions;
    private final EmailGateway email;
    private final BarizoneProperties properties;

    public SubscriberService(NewsSubscriberRepository subscribers, SubscriberConsentRepository consents,
                             PushSubscriptionRepository pushSubscriptions, EmailGateway email,
                             BarizoneProperties properties) {
        this.subscribers = subscribers;
        this.consents = consents;
        this.pushSubscriptions = pushSubscriptions;
        this.email = email;
        this.properties = properties;
    }

    public Map<String, Object> config() {
        return Map.of("confirmation_email_enabled", email.enabled(), "contact_email", properties.contactEmail());
    }

    public record SubscribeResult(boolean created, Map<String, Object> body) {
    }

    @Transactional
    public SubscribeResult subscribe(SubscribeRequest b) {
        String first = Texts.clean(b.firstName());
        String last = Texts.clean(b.lastName());
        String mail = Texts.clean(b.email()).toLowerCase();
        String phone = Texts.clean(b.phone());
        String q = Texts.clean(b.quartiere());
        if (q.isEmpty()) {
            q = District.ALL;
        }
        boolean invalid = first.isEmpty() || first.length() > 80 || last.isEmpty() || last.length() > 80
                || !Texts.EMAIL.matcher(mail).matches() || mail.length() > 254
                || phone.length() > 25 || (!phone.isEmpty() && !phone.matches("^[+\\d\\s().-]{6,25}$"))
                || q.length() > 80 || !Boolean.TRUE.equals(b.consent())
                || !Texts.clean(b.website()).isEmpty(); // honeypot anti-bot
        if (invalid) {
            throw ApiException.badRequest("Controlla nome, cognome, email e consenso.");
        }
        if (subscribers.existsByEmail(mail)) {
            // Risposta identica per iscritti e non: non si rivela se un indirizzo è già presente.
            return new SubscribeResult(false, Map.of("ok", true,
                    "message", "Richiesta registrata. Se già iscritto, i dati precedenti restano invariati."));
        }
        if (b.consentVersion() != null && !CONSENT_VERSION.equals(b.consentVersion())) {
            throw ApiException.badRequest("Versione del consenso non valida.");
        }
        NewsSubscriber s = new NewsSubscriber();
        s.setId(UUID.randomUUID());
        s.setFirstName(first);
        s.setLastName(last);
        s.setEmail(mail);
        s.setPhone(phone);
        s.setQuartiere(q);
        s.setConsentAt(Instant.now());
        s.setStatus(SubscriberStatus.active);
        s.setToken(UUID.randomUUID().toString() + UUID.randomUUID());
        subscribers.save(s);
        if (CONSENT_VERSION.equals(b.consentVersion())) {
            consents.save(new SubscriberConsent(s.getId(), CONSENT_VERSION, s.getConsentAt()));
        }
        String status = email.sendBatch(welcomeEmails(s), "news-signup/" + s.getId());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", true);
        body.put("token", s.getToken());
        body.put("email_status", status);
        return new SubscribeResult(true, body);
    }

    /** Disiscrizione con il token personale: rimuove anche i dispositivi push collegati. */
    @Transactional
    public void unsubscribe(Object token) {
        if (!(token instanceof String t) || t.length() > 100) {
            throw ApiException.badRequest("Codice non valido.");
        }
        subscribers.findByToken(t).ifPresent(s -> {
            pushSubscriptions.deleteByUserId("subscriber:" + s.getId());
            s.setStatus(SubscriberStatus.unsubscribed);
        });
    }

    @Transactional(readOnly = true)
    public List<SubscriberAdminResponse> adminList() {
        return subscribers.adminListing().stream().map(r -> new SubscriberAdminResponse(
                String.valueOf(r[0]), (String) r[1], (String) r[2], (String) r[3], (String) r[4], (String) r[5],
                toInstant(r[6]), (String) r[7], (String) r[8], ((Number) r[9]).intValue(), ((Number) r[10]).longValue()))
                .toList();
    }

    private static Instant toInstant(Object value) {
        return switch (value) {
            case Instant i -> i;
            case OffsetDateTime o -> o.toInstant();
            case Timestamp t -> t.toInstant();
            default -> null;
        };
    }

    private List<EmailGateway.Email> welcomeEmails(NewsSubscriber s) {
        String site = properties.publicOrigin();
        String contact = properties.contactEmail();
        String from = properties.email().from();
        return List.of(
                new EmailGateway.Email(from, List.of(s.getEmail()), contact, "Grazie per esserti iscritto a Barizone!",
                        "Ciao " + s.getFirstName() + ",\n\ngrazie per essere entrato nella comunità di Barizone! "
                                + "Abbiamo registrato la tua iscrizione alle news di Barizone per il rione " + s.getQuartiere() + ".\n\n"
                                + "Vivi Bari, rione per rione: " + site + "/\n"
                                + "Per ricevere le notifiche sul telefono, attivale con una scelta separata: " + site + "/notifiche\n"
                                + "Per scriverci: " + site + "/contatti\n"
                                + "Per comunicazioni o per annullare l’iscrizione, scrivi a " + contact
                                + " dall’indirizzo iscritto oppure usa " + site + "/iscriviti-news.\n\nLa redazione di Barizone"),
                new EmailGateway.Email(from, List.of(contact), s.getEmail(), "Nuova iscrizione alle news di Barizone",
                        "Nuova iscrizione registrata sul sito.\n\nNome: " + s.getFirstName() + " " + s.getLastName()
                                + "\nEmail: " + s.getEmail() + "\nRione: " + s.getQuartiere()
                                + "\nConsenso registrato: " + s.getConsentAt()
                                + "\n\nElenco completo nell’area riservata: " + site + "/admin"));
    }
}
