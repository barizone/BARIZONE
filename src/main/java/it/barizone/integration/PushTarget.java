package it.barizone.integration;

/** Destinazione Web Push (dalla PushSubscription del browser). */
public record PushTarget(String endpoint, String p256dh, String auth) {
}
