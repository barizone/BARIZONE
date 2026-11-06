package it.barizone.service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import it.barizone.dto.request.EndpointRequest;
import it.barizone.dto.request.PushSubscribeRequest;
import it.barizone.exception.ApiException;
import it.barizone.integration.PushTarget;
import it.barizone.integration.VapidKeyProvider;
import it.barizone.model.entity.PushSubscription;
import it.barizone.model.enums.District;
import it.barizone.model.enums.SubscriberStatus;
import it.barizone.repository.NewsSubscriberRepository;
import it.barizone.repository.PushSubscriptionRepository;
import it.barizone.util.Tokens;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Iscrizione dei dispositivi alle notifiche di Barizone, con consenso marketing esplicito e revocabile. */
@Service
public class PushSubscriptionService {

    public static final String CONSENT_VERSION = "marketing-v1";

    private final PushSubscriptionRepository subscriptions;
    private final NewsSubscriberRepository subscribers;
    private final VapidKeyProvider keys;

    public PushSubscriptionService(PushSubscriptionRepository subscriptions, NewsSubscriberRepository subscribers,
                                   VapidKeyProvider keys) {
        this.subscriptions = subscriptions;
        this.subscribers = subscribers;
        this.keys = keys;
    }

    public String publicKey() {
        return keys.keys().publicKey();
    }

    public static String idFor(String endpoint) {
        return Tokens.sha256Base64Url(endpoint);
    }

    @Transactional
    public void subscribe(PushSubscribeRequest body) {
        PushTarget target = PushValidation.parse(body.subscription());
        if (!Boolean.TRUE.equals(body.marketingConsent()) || !CONSENT_VERSION.equals(body.consentVersion())
                || !(body.quartiere() instanceof String q) || !District.isValid(q) || target == null) {
            throw ApiException.badRequest("Per attivare le notifiche serve il consenso specifico al marketing e una sottoscrizione valida.");
        }
        String userId = "public-device";
        if (body.subscriberToken() instanceof String token && token.length() <= 100) {
            userId = subscribers.findByTokenAndStatus(token, SubscriberStatus.active)
                    .map(s -> "subscriber:" + s.getId()).orElse(userId);
        }
        PushSubscription s = subscriptions.findById(idFor(target.endpoint())).orElseGet(PushSubscription::new);
        s.setId(idFor(target.endpoint()));
        s.setUserId(userId);
        s.setEndpoint(target.endpoint());
        s.setP256dh(target.p256dh());
        s.setAuth(target.auth());
        s.setQuartiere(q);
        s.setConsentedAt(Instant.now());
        s.setConsentVersion(CONSENT_VERSION);
        subscriptions.save(s);
    }

    @Transactional
    public void revoke(EndpointRequest body) {
        subscriptions.deleteById(idFor(endpoint(body)));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> status(EndpointRequest body) {
        PushSubscription s = subscriptions.findById(idFor(endpoint(body))).orElse(null);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("active", s != null);
        if (s != null) {
            result.put("quartiere", s.getQuartiere());
        }
        return result;
    }

    static String endpoint(EndpointRequest body) {
        if (!(body.endpoint() instanceof String endpoint)) {
            throw ApiException.badRequest("Dispositivo non valido");
        }
        return endpoint;
    }
}
