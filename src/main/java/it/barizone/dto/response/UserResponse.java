package it.barizone.dto.response;

/** Utente autenticato, nel formato atteso dal frontend ({@code /api/auth/me}). */
public record UserResponse(String id, String name, String email, String role) {
}
