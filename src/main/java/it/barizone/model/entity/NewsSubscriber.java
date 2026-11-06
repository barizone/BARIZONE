package it.barizone.model.entity;

import java.time.Instant;
import java.util.UUID;

import it.barizone.model.enums.SubscriberStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "news_subscribers")
@Getter
@Setter
@NoArgsConstructor
public class NewsSubscriber {

    @Id
    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone = "";
    private String quartiere;
    private Instant consentAt;

    @Enumerated(EnumType.STRING)
    private SubscriberStatus status;

    private String token;
}
