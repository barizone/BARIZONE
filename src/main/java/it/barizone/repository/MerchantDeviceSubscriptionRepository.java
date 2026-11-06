package it.barizone.repository;

import java.util.Optional;
import java.util.UUID;

import it.barizone.model.entity.MerchantDeviceSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.Query;

public interface MerchantDeviceSubscriptionRepository extends JpaRepository<MerchantDeviceSubscription, String> {

    Optional<MerchantDeviceSubscription> findByClientId(UUID clientId);

    boolean existsByClientId(UUID clientId);

    @Transactional
    @Modifying
    @Query("DELETE FROM MerchantDeviceSubscription s WHERE s.clientId = :clientId OR s.id = :id")
    int deleteByClientIdOrId(UUID clientId, String id);

    @Transactional
    @Modifying
    @Query("DELETE FROM MerchantDeviceSubscription s WHERE s.clientId = :clientId")
    int deleteByClientId(UUID clientId);
}
