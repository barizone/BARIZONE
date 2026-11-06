package it.barizone.model.entity;

import java.time.Instant;
import java.util.UUID;

import it.barizone.model.enums.AccountRole;
import it.barizone.model.enums.AccountStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Account di un utente o di un'attività. L'email è sempre salvata in minuscolo. */
@Entity
@Table(name = "user_accounts")
@Getter
@Setter
@NoArgsConstructor
public class UserAccount {

    @Id
    private UUID id;
    private String email;
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    private AccountRole role;

    private String firstName;
    private String lastName;

    @Enumerated(EnumType.STRING)
    private AccountStatus status;

    private Instant emailVerifiedAt;
    private String termsVersion;
    private Instant termsAcceptedAt;
    private Instant createdAt;
    private Instant lastLoginAt;

    public String fullName() {
        return (firstName + " " + lastName).trim();
    }
}
