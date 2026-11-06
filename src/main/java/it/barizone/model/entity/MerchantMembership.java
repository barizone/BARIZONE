package it.barizone.model.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "merchant_memberships")
@Getter
@Setter
@NoArgsConstructor
public class MerchantMembership {

    @Id
    private UUID id;
    private UUID clientId;
    private String shopId;
    private String alias;
    private boolean active = true;
    private Instant updatedAt;
}
