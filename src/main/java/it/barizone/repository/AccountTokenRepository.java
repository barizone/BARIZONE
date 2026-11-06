package it.barizone.repository;

import java.util.Optional;
import java.util.UUID;

import it.barizone.model.entity.AccountToken;
import it.barizone.model.enums.TokenPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface AccountTokenRepository extends JpaRepository<AccountToken, String> {

    Optional<AccountToken> findByTokenHashAndPurpose(String tokenHash, TokenPurpose purpose);

    /** Un nuovo link annulla quelli precedenti non ancora usati. */
    @Transactional
    @Modifying
    @Query("DELETE FROM AccountToken t WHERE t.accountId = :accountId AND t.purpose = :purpose AND t.usedAt IS NULL")
    int deleteUnused(UUID accountId, TokenPurpose purpose);
}
