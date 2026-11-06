package it.barizone.model.entity;

import java.time.Instant;
import java.util.UUID;

import it.barizone.model.enums.DeliveryStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "push_deliveries")
@Getter
@Setter
@NoArgsConstructor
public class PushDelivery {

    @Id
    private String id;
    private UUID campaignId;
    private String subscriptionId;

    @Enumerated(EnumType.STRING)
    private DeliveryStatus status;

    private Instant sentAt;
}
