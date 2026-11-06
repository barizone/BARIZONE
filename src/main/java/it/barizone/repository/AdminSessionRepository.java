package it.barizone.repository;

import java.time.Instant;
import java.util.Optional;

import it.barizone.model.entity.AdminSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.Query;

public interface AdminSessionRepository extends JpaRepository<AdminSession, String> {

    Optional<AdminSession> findByTokenHashAndExpiresAtAfter(String tokenHash, Instant now);

    @Transactional
    @Modifying
    @Query("DELETE FROM AdminSession s WHERE s.expiresAt <= :now")
    int deleteExpired(Instant now);
}
