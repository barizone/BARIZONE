package it.barizone.repository;

import java.time.Instant;
import java.util.UUID;

import it.barizone.model.enums.DeliveryStatus;
import it.barizone.model.entity.PushDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.Query;

public interface PushDeliveryRepository extends JpaRepository<PushDelivery, String> {

    long countByCampaignIdAndStatus(UUID campaignId, DeliveryStatus status);

    long countByCampaignIdAndStatusNot(UUID campaignId, DeliveryStatus status);

    /** Prenota atomicamente una consegna: 1 se inserita, 0 se già presa da un altro processo. */
    @Transactional
    @Modifying
    @Query(value = """
            INSERT INTO push_deliveries (id, campaign_id, subscription_id, status, sent_at)
            VALUES (:id, :campaignId, :subscriptionId, 'sending', :sentAt)
            ON CONFLICT (id) DO NOTHING
            """, nativeQuery = true)
    int claim(String id, UUID campaignId, String subscriptionId, Instant sentAt);

    @Transactional
    @Modifying
    @Query("UPDATE PushDelivery d SET d.status = :status WHERE d.id = :id")
    int updateStatus(String id, DeliveryStatus status);
}
