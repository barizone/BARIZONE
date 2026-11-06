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

/** Cliente anonimo che aderisce a una o più attività (identificato solo dal cookie). */
@Entity
@Table(name = "merchant_clients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MerchantClient {

    @Id
    private UUID id;
    private String tokenHash;
    private Instant createdAt;
}
