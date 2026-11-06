package it.barizone.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import it.barizone.model.entity.NewsSubscriber;
import it.barizone.model.enums.SubscriberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface NewsSubscriberRepository extends JpaRepository<NewsSubscriber, UUID> {

    boolean existsByEmail(String email);

    Optional<NewsSubscriber> findByToken(String token);

    Optional<NewsSubscriber> findByTokenAndStatus(String token, SubscriberStatus status);

    /** Elenco per l'admin con versione del consenso e dispositivi push collegati. */
    @Query(value = """
            SELECT n.id, n.first_name, n.last_name, n.email, n.phone, n.quartiere, n.consent_at, n.status,
                   c.version AS consent_version,
                   CASE WHEN c.version = 'barizone-updates-v1' AND n.status = 'active' THEN 1 ELSE 0 END AS push_requested,
                   (SELECT COUNT(*) FROM push_subscriptions p WHERE p.user_id = 'subscriber:' || n.id) AS push_devices
            FROM news_subscribers n
            LEFT JOIN news_subscriber_consents c ON c.subscriber_id = n.id
            ORDER BY n.consent_at DESC
            """, nativeQuery = true)
    List<Object[]> adminListing();
}
