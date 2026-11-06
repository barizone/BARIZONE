package it.barizone.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import it.barizone.model.entity.AccountSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface AccountSessionRepository extends JpaRepository<AccountSession, String> {

    Optional<AccountSession> findByTokenHashAndExpiresAtAfter(String tokenHash, Instant now);

    @Transactional
    @Modifying
    @Query("DELETE FROM AccountSession s WHERE s.accountId = :accountId")
    int deleteByAccountId(UUID accountId);

    @Transactional
    @Modifying
    @Query("DELETE FROM AccountSession s WHERE s.expiresAt <= :now")
    int deleteExpired(Instant now);
}
