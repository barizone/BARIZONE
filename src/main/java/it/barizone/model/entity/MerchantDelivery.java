package it.barizone.model.entity;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

import it.barizone.model.enums.DeliveryStatus;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "merchant_deliveries")
@Getter
@Setter
@NoArgsConstructor
public class MerchantDelivery {

    @EmbeddedId
    private Key id;
    private Instant deliveredAt;

    @Enumerated(EnumType.STRING)
    private DeliveryStatus pushStatus = DeliveryStatus.pending;

    @Embeddable
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Key implements Serializable {
        private UUID messageId;
        private UUID membershipId;
    }
}
