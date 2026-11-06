package it.barizone.integration;

/** Contenuto della notifica, letto dal service worker ({@code push-sw.js}). */
public record PushMessage(String title, String body, String url, String tag) {
}
