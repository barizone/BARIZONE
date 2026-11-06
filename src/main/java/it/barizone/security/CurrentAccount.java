package it.barizone.security;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Account (utente o attività) della richiesta corrente, se autenticato. */
public final class CurrentAccount {

    private CurrentAccount() {
    }

    public static Optional<AccountPrincipal> get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof AccountPrincipal account
                ? Optional.of(account) : Optional.empty();
    }
}
