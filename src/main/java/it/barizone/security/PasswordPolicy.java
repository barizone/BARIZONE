package it.barizone.security;

import java.nio.charset.StandardCharsets;

import it.barizone.exception.ApiException;

/** Regole minime per le password: lunghezza, almeno una lettera e una cifra, diversa dall'email. */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 10;
    /** BCrypt considera solo i primi 72 byte: oltre questo limite la password verrebbe troncata in silenzio. */
    public static final int MAX_BYTES = 72;

    private PasswordPolicy() {
    }

    public static void check(String password, String email) {
        String p = password == null ? "" : password;
        if (p.length() < MIN_LENGTH) {
            throw ApiException.badRequest("La password deve avere almeno " + MIN_LENGTH + " caratteri.");
        }
        if (p.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw ApiException.badRequest("La password è troppo lunga (massimo " + MAX_BYTES + " caratteri).");
        }
        if (!p.matches(".*\\p{L}.*") || !p.matches(".*\\d.*")) {
            throw ApiException.badRequest("La password deve contenere almeno una lettera e un numero.");
        }
        if (email != null && p.equalsIgnoreCase(email)) {
            throw ApiException.badRequest("La password non può coincidere con l’email.");
        }
    }
}
