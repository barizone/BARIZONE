package it.barizone.repository;

import java.util.Optional;
import java.util.UUID;

import it.barizone.model.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

    Optional<UserAccount> findByEmail(String email);
}
