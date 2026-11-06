package it.barizone.repository;

import java.time.Instant;
import java.util.Optional;

import it.barizone.model.entity.MerchantSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.Query;

public interface MerchantSessionRepository extends JpaRepository<MerchantSession, String> {

    Optional<MerchantSession> findByTokenHashAndExpiresAtAfter(String tokenHash, Instant now);

    /** Rotazione del token monouso: riesce una sola volta. */
    @Transactional
    @Modifying
    @Query(value = """
            UPDATE merchant_sessions SET token_hash = :newHash, expires_at = :expiresAt
            WHERE token_hash = :oldHash AND expires_at > :now
            """, nativeQuery = true)
    int rotate(String oldHash, String newHash, Instant expiresAt, Instant now);

    @Transactional
    @Modifying
    @Query("DELETE FROM MerchantSession s WHERE s.expiresAt <= :now")
    int deleteExpired(Instant now);
}
