package it.barizone.model.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Dispositivo del cliente per le comunicazioni degli esercenti a cui ha aderito. */
@Entity
@Table(name = "merchant_device_subscriptions")
@Getter
@Setter
@NoArgsConstructor
public class MerchantDeviceSubscription {

    @Id
    private String id;
    private UUID clientId;
    private String endpoint;
    private String p256dh;
    private String auth;
    private Instant consentedAt;
}
