package it.barizone.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import it.barizone.model.entity.PushSubscription;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.Query;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, String> {

    @Transactional
    @Modifying
    @Query("DELETE FROM PushSubscription s WHERE s.userId = :userId")
    int deleteByUserId(String userId);

    @Query("SELECT s.quartiere AS quartiere, COUNT(s) AS total FROM PushSubscription s GROUP BY s.quartiere")
    List<DistrictCount> countByDistrict();

    /**
     * Destinatari non ancora raggiunti da una campagna: iscritti prima dell'avvio, nel quartiere
     * della campagna oppure in "Tutta Bari" (o tutti, se la campagna è per tutta la città).
     */
    @Query("""
            SELECT s FROM PushSubscription s
            WHERE s.consentedAt <= :startedAt
              AND (:quartiere = 'Tutta Bari' OR s.quartiere = :quartiere OR s.quartiere = 'Tutta Bari')
              AND NOT EXISTS (SELECT 1 FROM PushDelivery d WHERE d.subscriptionId = s.id AND d.campaignId = :campaignId)
            ORDER BY s.id
            """)
    List<PushSubscription> pendingForCampaign(Instant startedAt, String quartiere, UUID campaignId, Limit limit);

    interface DistrictCount {
        String getQuartiere();

        long getTotal();
    }
}
