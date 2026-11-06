package it.barizone.security;

/** Amministratore autenticato. {@code id} è lo username, oppure "admin" per l'accesso con sola password. */
public record AdminPrincipal(String id, String name, String email) {
}
