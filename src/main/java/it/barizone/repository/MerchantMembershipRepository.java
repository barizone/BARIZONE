package it.barizone.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import it.barizone.model.entity.MerchantMembership;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantMembershipRepository extends JpaRepository<MerchantMembership, UUID> {

    List<MerchantMembership> findAllByOrderByUpdatedAtAsc();

    List<MerchantMembership> findByShopIdOrderByUpdatedAtAsc(String shopId);

    List<MerchantMembership> findByShopIdAndActiveTrue(String shopId);

    long countByShopIdAndActiveTrue(String shopId);

    List<MerchantMembership> findByClientId(UUID clientId);

    boolean existsByClientIdAndActiveTrue(UUID clientId);

    Optional<MerchantMembership> findByClientIdAndShopId(UUID clientId, String shopId);
}
