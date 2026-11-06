package it.barizone.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import it.barizone.model.entity.MerchantMessage;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantMessageRepository extends JpaRepository<MerchantMessage, UUID> {

    List<MerchantMessage> findAllByOrderByCreatedAtDesc(Limit limit);

    List<MerchantMessage> findByShopIdOrderByCreatedAtDesc(String shopId, Limit limit);

    Optional<MerchantMessage> findByIdAndShopId(UUID id, String shopId);
}
