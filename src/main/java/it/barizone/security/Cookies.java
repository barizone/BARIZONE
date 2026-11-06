package it.barizone.security;

import java.util.Arrays;
import java.util.Optional;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;

/**
 * Cookie di sessione con prefisso {@code __Host-}: Secure, HttpOnly, Path=/, nessun Domain.
 * Nomi e attributi coincidono con la versione Worker, quindi le sessioni restano compatibili.
 */
public final class Cookies {

    public static final String ADMIN = "__Host-barizone_session";
    public static final String ACCOUNT = "__Host-barizone_account";
    public static final String CUSTOMER = "__Host-barizone_customer";

    private Cookies() {
    }

    public static Optional<String> read(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies).filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue).filter(v -> !v.isEmpty()).findFirst();
    }

    public static String lax(String name, String value, long maxAgeSeconds) {
        return build(name, value, maxAgeSeconds, "Lax");
    }

    public static String strict(String name, String value, long maxAgeSeconds) {
        return build(name, value, maxAgeSeconds, "Strict");
    }

    private static String build(String name, String value, long maxAgeSeconds, String sameSite) {
        return ResponseCookie.from(name, value).path("/").httpOnly(true).secure(true)
                .sameSite(sameSite).maxAge(maxAgeSeconds).build().toString();
    }
}
