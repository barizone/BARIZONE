package it.barizone.repository;

import java.util.UUID;

import it.barizone.model.entity.SubscriberConsent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriberConsentRepository extends JpaRepository<SubscriberConsent, UUID> {
}
