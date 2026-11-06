package it.barizone.repository;

import java.time.LocalDate;
import java.util.Optional;

import it.barizone.model.entity.BusinessSpotlight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.Query;

public interface BusinessSpotlightRepository extends JpaRepository<BusinessSpotlight, LocalDate> {

    Optional<BusinessSpotlight> findFirstBySpotlightDateBeforeOrderBySpotlightDateDesc(LocalDate date);

    /**
     * Fissa l'azienda del giorno. Se un'altra richiesta l'ha già fissata, vince la prima;
     * si sostituisce solo la scelta {@code expected} (non più pubblicata).
     */
    @Transactional
    @Modifying
    @Query(value = """
            INSERT INTO business_spotlight (spotlight_date, merchant_id) VALUES (:date, :merchantId)
            ON CONFLICT (spotlight_date) DO UPDATE SET merchant_id = EXCLUDED.merchant_id
            WHERE business_spotlight.merchant_id = :expected
            """, nativeQuery = true)
    int upsert(LocalDate date, String merchantId, String expected);
}
