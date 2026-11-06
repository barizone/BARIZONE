package it.barizone.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import it.barizone.model.entity.MerchantDelivery;
import it.barizone.model.enums.DeliveryStatus;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.Query;

public interface MerchantDeliveryRepository extends JpaRepository<MerchantDelivery, MerchantDelivery.Key> {

    List<MerchantDelivery> findByIdMessageIdIn(Collection<UUID> messageIds);

    List<MerchantDelivery> findByIdMessageIdAndPushStatus(UUID messageId, DeliveryStatus status, Limit limit);

    List<MerchantDelivery> findByIdMessageId(UUID messageId);

    /** Prenota una consegna in attesa: 1 se riuscito, 0 se presa da un altro processo. */
    @Transactional
    @Modifying
    @Query("""
            UPDATE MerchantDelivery d SET d.pushStatus = it.barizone.model.enums.DeliveryStatus.sending
            WHERE d.id = :key AND d.pushStatus = it.barizone.model.enums.DeliveryStatus.pending
            """)
    int claim(MerchantDelivery.Key key);

    @Query("SELECT DISTINCT d.id.messageId FROM MerchantDelivery d WHERE d.pushStatus = it.barizone.model.enums.DeliveryStatus.pending")
    List<UUID> messagesWithPending();

    /** Comunicazioni ricevute da un cliente, per le attività a cui aderisce. */
    @Query(value = """
            SELECT n.id, n.shop_id, n.title, n.body, n.created_at, d.push_status
            FROM merchant_deliveries d
            JOIN merchant_memberships m ON m.id = d.membership_id
            JOIN merchant_messages n ON n.id = d.message_id
            WHERE m.client_id = :clientId AND n.shop_id = m.shop_id
            ORDER BY n.created_at DESC LIMIT 100
            """, nativeQuery = true)
    List<Object[]> inboxFor(UUID clientId);

    @Transactional
    @Modifying
    @Query("UPDATE MerchantDelivery d SET d.pushStatus = :status WHERE d.id = :key")
    int updateStatus(MerchantDelivery.Key key, DeliveryStatus status);
}
