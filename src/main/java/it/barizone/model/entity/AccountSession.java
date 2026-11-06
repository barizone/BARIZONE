package it.barizone.model.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Sessione: nel database c'è solo l'hash SHA-256 del token, mai il token. */
@Entity
@Table(name = "account_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccountSession {

    @Id
    private String tokenHash;
    private UUID accountId;
    private Instant expiresAt;
}
