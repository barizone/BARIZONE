package it.barizone.security;

import java.util.UUID;

import it.barizone.model.enums.AccountRole;

/** Utente o attività autenticati con il proprio account. */
public record AccountPrincipal(UUID id, String email, String name, AccountRole role) {
}
