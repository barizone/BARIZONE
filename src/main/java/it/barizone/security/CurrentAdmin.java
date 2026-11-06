package it.barizone.security;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Accesso statico all'amministratore della richiesta corrente (se presente). */
public final class CurrentAdmin {

    private CurrentAdmin() {
    }

    public static Optional<AdminPrincipal> get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof AdminPrincipal admin
                ? Optional.of(admin) : Optional.empty();
    }

    public static boolean isAdmin() {
        return get().isPresent();
    }
}
