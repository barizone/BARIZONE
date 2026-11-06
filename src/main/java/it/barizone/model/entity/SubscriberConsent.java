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

/** Versione del consenso accettata dall'iscritto, con data. */
@Entity
@Table(name = "news_subscriber_consents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubscriberConsent {

    @Id
    private UUID subscriberId;
    private String version;
    private Instant consentAt;
}
