package it.barizone.model.entity;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Dispositivo iscritto alle notifiche di Barizone (consenso marketing). */
@Entity
@Table(name = "push_subscriptions")
@Getter
@Setter
@NoArgsConstructor
public class PushSubscription {

    @Id
    private String id;
    private String userId;
    private String endpoint;
    private String p256dh;
    private String auth;
    private String quartiere;
    private Instant consentedAt;
    private String consentVersion;
}
