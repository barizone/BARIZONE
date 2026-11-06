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

@Entity
@Table(name = "merchant_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MerchantMessage {

    @Id
    private UUID id;
    private String shopId;
    private String title;
    private String body;
    private Instant createdAt;
}
