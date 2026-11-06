package it.barizone.repository;

import java.util.Optional;
import java.util.UUID;

import it.barizone.model.entity.MerchantClient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantClientRepository extends JpaRepository<MerchantClient, UUID> {

    Optional<MerchantClient> findByTokenHash(String tokenHash);
}
