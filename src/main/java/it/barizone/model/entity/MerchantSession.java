package it.barizone.model.entity;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Accesso esercente: link monouso generato dall'admin, poi sessione da 12 ore. */
@Entity
@Table(name = "merchant_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MerchantSession {

    @Id
    private String tokenHash;
    private String shopId;
    private Instant expiresAt;
}
