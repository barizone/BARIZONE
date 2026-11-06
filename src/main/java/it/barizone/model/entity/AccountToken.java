package it.barizone.model.entity;

import java.time.Instant;
import java.util.UUID;

import it.barizone.model.enums.TokenPurpose;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Link monouso (verifica email o recupero password). Salvato solo come hash. */
@Entity
@Table(name = "account_tokens")
@Getter
@Setter
@NoArgsConstructor
public class AccountToken {

    @Id
    private String tokenHash;
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    private TokenPurpose purpose;

    private Instant expiresAt;
    private Instant usedAt;
}
