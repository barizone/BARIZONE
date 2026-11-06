package it.barizone.integration;

/** Chiavi VAPID in base64url: pubblica = punto P-256 non compresso (65 byte), privata = scalare d (32 byte). */
public record VapidKeys(String publicKey, String privateKey, String subject) {
}
