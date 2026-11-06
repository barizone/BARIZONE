package it.barizone.model.entity;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Sessione admin: nel DB c'è solo l'hash SHA-256 del token, mai il token. */
@Entity
@Table(name = "admin_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminSession {

    @Id
    private String tokenHash;
    private String username;
    private Instant expiresAt;
}
