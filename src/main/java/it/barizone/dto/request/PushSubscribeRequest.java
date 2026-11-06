package it.barizone.dto.request;

public record PushSubscribeRequest(Object marketingConsent, Object consentVersion, Object quartiere,
                                   Object subscription, Object subscriberToken) {
}
