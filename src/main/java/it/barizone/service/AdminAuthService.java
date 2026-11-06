package it.barizone.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import it.barizone.configuration.BarizoneProperties;
import it.barizone.dto.response.UserResponse;
import it.barizone.exception.ApiException;
import it.barizone.model.entity.AdminAccount;
import it.barizone.model.entity.AdminSession;
import it.barizone.model.entity.LoginAttempt;
import it.barizone.repository.AdminAccountRepository;
import it.barizone.repository.AdminSessionRepository;
import it.barizone.repository.LoginAttemptRepository;
import it.barizone.security.AdminPrincipal;
import it.barizone.security.LegacyAdminToken;
import it.barizone.security.PasswordHasher;
import it.barizone.util.Tokens;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Accesso amministrativo. Due modalità, come nella versione Worker:
 * <ol>
 *   <li>account nel database (username + password PBKDF2), con sessione opaca e limite di tentativi;</li>
 *   <li>accesso di riserva con la sola {@code ADMIN_PASSWORD}, firmato con {@code SESSION_SECRET}.</li>
 * </ol>
 */
@Service
public class AdminAuthService {

    private static final String DUMMY_SALT = "0".repeat(48);

    private final AdminAccountRepository accounts;
    private final AdminSessionRepository sessions;
    private final LoginAttemptRepository attempts;
    private final PasswordHasher hasher;
    private final LegacyAdminToken legacyToken;
    private final BarizoneProperties properties;

    public AdminAuthService(AdminAccountRepository accounts, AdminSessionRepository sessions,
                            LoginAttemptRepository attempts, PasswordHasher hasher,
                            LegacyAdminToken legacyToken, BarizoneProperties properties) {
        this.accounts = accounts;
        this.sessions = sessions;
        this.attempts = attempts;
        this.hasher = hasher;
        this.legacyToken = legacyToken;
        this.properties = properties;
    }

    public record LoginResult(UserResponse user, String token) {
    }

    /** Identità associata al valore del cookie di sessione, se valido. */
    @Transactional(readOnly = true)
    public Optional<AdminPrincipal> resolve(String token) {
        if (Tokens.isHexToken(token)) {
            return sessions.findByTokenHashAndExpiresAtAfter(Tokens.sha256Hex(token), Instant.now())
                    .flatMap(s -> accounts.findById(s.getUsername()))
                    .map(a -> new AdminPrincipal(a.getUsername(), a.getName(), adminEmail()));
        }
        String secret = properties.admin().sessionSecret();
        if (secret != null && !secret.isBlank() && legacyToken.verify(secret, token)) {
            return Optional.of(new AdminPrincipal("admin", "Amministratore", adminEmail()));
        }
        return Optional.empty();
    }

    @Transactional(noRollbackFor = ApiException.class)
    public LoginResult login(String username, String password, String clientIp) {
        String pwd = password == null ? "" : password;
        if (pwd.length() > 1024) {
            throw ApiException.unauthorized("Credenziali non valide.");
        }
        if (username != null && !username.isBlank()) {
            return accountLogin(username.trim().toLowerCase(), pwd, clientIp);
        }
        return legacyLogin(pwd);
    }

    private LoginResult accountLogin(String username, String password, String clientIp) {
        if (username.length() > 80) {
            throw ApiException.unauthorized("Credenziali non valide.");
        }
        Instant now = Instant.now();
        Duration window = Duration.ofMinutes(properties.admin().loginWindowMinutes());
        String attemptKey = Tokens.sha256Hex(username + ":" + (clientIp == null ? "unknown" : clientIp));
        Optional<LoginAttempt> previous = attempts.findById(attemptKey);
        if (previous.isPresent() && previous.get().getWindowStart().plus(window).isAfter(now)
                && previous.get().getAttempts() >= properties.admin().maxLoginAttempts()) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Troppi tentativi. Riprova tra 15 minuti.");
        }
        Optional<AdminAccount> account = accounts.findById(username);
        // L'hash viene calcolato anche per username inesistenti: tempi di risposta uniformi.
        String computed = hasher.hash(password,
                account.map(AdminAccount::getSalt).orElse(DUMMY_SALT),
                account.map(AdminAccount::getIterations).orElse(100_000));
        if (account.isEmpty() || !Tokens.constantTimeEquals(computed, account.get().getPasswordHash())) {
            LoginAttempt attempt = previous.orElseGet(() -> new LoginAttempt(attemptKey, 0, now));
            if (!attempt.getWindowStart().plus(window).isAfter(now)) {
                attempt.setAttempts(0);
                attempt.setWindowStart(now);
            }
            attempt.setAttempts(attempt.getAttempts() + 1);
            attempts.save(attempt);
            throw ApiException.unauthorized("Credenziali non valide.");
        }
        previous.ifPresent(attempts::delete);
        sessions.deleteExpired(now);
        String token = Tokens.randomToken();
        sessions.save(new AdminSession(Tokens.sha256Hex(token), username,
                now.plusSeconds(properties.admin().sessionSeconds())));
        return new LoginResult(new UserResponse(username, account.get().getName(), adminEmail(), "admin"), token);
    }

    private LoginResult legacyLogin(String password) {
        String expected = properties.admin().password();
        String secret = properties.admin().sessionSecret();
        if (expected == null || expected.isBlank() || secret == null || secret.isBlank()) {
            throw ApiException.unauthorized("Inserisci nome utente e password amministratore.");
        }
        if (!Tokens.constantTimeEquals(password, expected)) {
            throw ApiException.unauthorized("Credenziali non valide.");
        }
        String token = legacyToken.create(secret, properties.admin().sessionSeconds());
        return new LoginResult(new UserResponse("admin", "Amministratore", adminEmail(), "admin"), token);
    }

    @Transactional
    public void logout(String token) {
        if (Tokens.isHexToken(token)) {
            sessions.deleteById(Tokens.sha256Hex(token));
        }
    }

    private String adminEmail() {
        return properties.admin().email() == null ? "" : properties.admin().email();
    }
}
