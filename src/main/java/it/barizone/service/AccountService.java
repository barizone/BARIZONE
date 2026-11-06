package it.barizone.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import it.barizone.configuration.BarizoneProperties;
import it.barizone.dto.request.AccountRequests;
import it.barizone.dto.response.AccountResponse;
import it.barizone.exception.ApiException;
import it.barizone.model.entity.AccountSession;
import it.barizone.model.entity.AccountToken;
import it.barizone.model.entity.CompanyProfile;
import it.barizone.model.entity.LoginAttempt;
import it.barizone.model.entity.UserAccount;
import it.barizone.model.enums.AccountRole;
import it.barizone.model.enums.AccountStatus;
import it.barizone.model.enums.TokenPurpose;
import it.barizone.repository.AccountSessionRepository;
import it.barizone.repository.AccountTokenRepository;
import it.barizone.repository.CompanyProfileRepository;
import it.barizone.repository.LoginAttemptRepository;
import it.barizone.repository.UserAccountRepository;
import it.barizone.security.AccountPrincipal;
import it.barizone.security.PasswordPolicy;
import it.barizone.util.Texts;
import it.barizone.util.Tokens;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Account di utenti e attività: registrazione, conferma email, accesso, recupero password.
 * L'amministratore ha un archivio separato ({@link AdminAuthService}).
 *
 * <p>Scelte di sicurezza: password con BCrypt; token di sessione e di verifica salvati solo come hash;
 * risposte identiche per email già registrata o no (non si rivela chi è iscritto); limite di 10 tentativi
 * di accesso ogni 15 minuti per coppia email+IP; hash calcolato anche per email inesistenti.
 */
@Service
public class AccountService {

    private static final Duration VERIFY_TTL = Duration.ofHours(48);
    private static final Duration RESET_TTL = Duration.ofHours(1);
    private static final String DUMMY_HASH = "$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5BcaWJ0pS4lEd0SSJ0KQ1ZLkCUZ7e";

    private final UserAccountRepository accounts;
    private final AccountSessionRepository sessions;
    private final AccountTokenRepository tokens;
    private final LoginAttemptRepository attempts;
    private final CompanyProfileRepository companies;
    private final PasswordEncoder encoder;
    private final AccountMailer mailer;
    private final BarizoneProperties properties;
    private final Clock clock;

    public AccountService(UserAccountRepository accounts, AccountSessionRepository sessions,
                          AccountTokenRepository tokens, LoginAttemptRepository attempts,
                          CompanyProfileRepository companies, PasswordEncoder encoder, AccountMailer mailer,
                          BarizoneProperties properties, Clock clock) {
        this.accounts = accounts;
        this.sessions = sessions;
        this.tokens = tokens;
        this.attempts = attempts;
        this.companies = companies;
        this.encoder = encoder;
        this.mailer = mailer;
        this.properties = properties;
        this.clock = clock;
    }

    /** Esito di una registrazione: {@code emailStatus} = accepted / failed / disabled / not_required. */
    public record Outcome(String emailStatus, boolean verificationRequired) {
    }

    public record LoginResult(String token, AccountResponse account) {
    }

    // ───────────── Registrazione ─────────────

    @Transactional
    public Outcome registerUser(AccountRequests.Register r) {
        String email = checkIdentity(r.firstName(), r.lastName(), r.email(), r.password(), r.acceptTerms());
        return register(email, r.password(), Texts.clean(r.firstName()), Texts.clean(r.lastName()), AccountRole.user, null);
    }

    /**
     * Crea l'account e, nella stessa transazione, l'eventuale scheda attività.
     * Se l'email è già registrata non crea nulla e risponde come se avesse creato:
     * chi è già iscritto riceve di nuovo il link di conferma se non l'aveva ancora usato.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public Outcome register(String email, String password, String first, String last, AccountRole role,
                            java.util.function.Consumer<UserAccount> afterCreate) {
        Optional<UserAccount> existing = accounts.findByEmail(email);
        if (existing.isPresent()) {
            UserAccount account = existing.get();
            return account.getStatus() == AccountStatus.pending_verification
                    ? new Outcome(sendVerification(account), true)
                    : new Outcome("not_required", false);
        }
        Instant now = clock.instant();
        boolean verify = properties.accounts().requireEmailVerification();
        UserAccount account = new UserAccount();
        account.setId(UUID.randomUUID());
        account.setEmail(email);
        account.setPasswordHash(encoder.encode(password));
        account.setRole(role);
        account.setFirstName(first);
        account.setLastName(last);
        account.setStatus(verify ? AccountStatus.pending_verification : AccountStatus.active);
        account.setTermsVersion(properties.accounts().termsVersion());
        account.setTermsAcceptedAt(now);
        account.setCreatedAt(now);
        // Due registrazioni simultanee con la stessa email: la seconda viola il vincolo di unicità
        // e riceve un errore 409 dal GlobalExceptionHandler.
        accounts.saveAndFlush(account);
        if (afterCreate != null) {
            afterCreate.accept(account);
        }
        return new Outcome(verify ? sendVerification(account) : "not_required", verify);
    }

    @Transactional
    public Outcome resendVerification(String emailInput) {
        String email = Texts.clean(emailInput).toLowerCase();
        Optional<UserAccount> account = accounts.findByEmail(email)
                .filter(a -> a.getStatus() == AccountStatus.pending_verification);
        return account.map(a -> new Outcome(sendVerification(a), true)).orElse(new Outcome("not_required", false));
    }

    /** Conferma l'email con il link ricevuto e attiva l'account. */
    @Transactional
    public AccountRole verifyEmail(String rawToken) {
        AccountToken token = usableToken(rawToken, TokenPurpose.verify_email);
        UserAccount account = accounts.findById(token.getAccountId())
                .orElseThrow(() -> ApiException.badRequest("Il collegamento non è valido o è scaduto."));
        token.setUsedAt(clock.instant());
        if (account.getStatus() == AccountStatus.pending_verification) {
            account.setStatus(AccountStatus.active);
            account.setEmailVerifiedAt(clock.instant());
        }
        return account.getRole();
    }

    // ───────────── Accesso ─────────────

    @Transactional(noRollbackFor = ApiException.class)
    public LoginResult login(AccountRequests.Login r, String ip) {
        String email = Texts.clean(r.email()).toLowerCase();
        String password = r.password() == null ? "" : r.password();
        if (email.length() > 254 || password.length() > 1024) {
            throw ApiException.unauthorized("Email o password non corrette.");
        }
        Instant now = clock.instant();
        Duration window = Duration.ofMinutes(properties.admin().loginWindowMinutes());
        String attemptKey = Tokens.sha256Hex("account:" + email + ":" + (ip == null ? "unknown" : ip));
        Optional<LoginAttempt> previous = attempts.findById(attemptKey);
        if (previous.isPresent() && previous.get().getWindowStart().plus(window).isAfter(now)
                && previous.get().getAttempts() >= properties.admin().maxLoginAttempts()) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Troppi tentativi. Riprova tra 15 minuti.");
        }
        Optional<UserAccount> account = accounts.findByEmail(email);
        // Il confronto avviene sempre, anche per email inesistenti: tempi di risposta uniformi.
        boolean ok = encoder.matches(password, account.map(UserAccount::getPasswordHash).orElse(DUMMY_HASH)) && account.isPresent();
        if (!ok) {
            LoginAttempt attempt = previous.orElseGet(() -> new LoginAttempt(attemptKey, 0, now));
            if (!attempt.getWindowStart().plus(window).isAfter(now)) {
                attempt.setAttempts(0);
                attempt.setWindowStart(now);
            }
            attempt.setAttempts(attempt.getAttempts() + 1);
            attempts.save(attempt);
            throw ApiException.unauthorized("Email o password non corrette.");
        }
        UserAccount a = account.get();
        if (a.getStatus() == AccountStatus.pending_verification) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Conferma prima il tuo indirizzo email con il link che ti abbiamo inviato.", java.util.Map.of("code", "email_not_verified"));
        }
        if (a.getStatus() == AccountStatus.suspended) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Account sospeso. Scrivi a " + properties.contactEmail() + ".");
        }
        previous.ifPresent(attempts::delete);
        sessions.deleteExpired(now);
        String token = Tokens.randomToken();
        sessions.save(new AccountSession(Tokens.sha256Hex(token), a.getId(), now.plusSeconds(properties.accounts().sessionSeconds())));
        a.setLastLoginAt(now);
        return new LoginResult(token, view(a));
    }

    @Transactional(readOnly = true)
    public Optional<AccountPrincipal> resolveSession(String token) {
        if (!Tokens.isHexToken(token)) {
            return Optional.empty();
        }
        return sessions.findByTokenHashAndExpiresAtAfter(Tokens.sha256Hex(token), clock.instant())
                .flatMap(s -> accounts.findById(s.getAccountId()))
                .filter(a -> a.getStatus() == AccountStatus.active)
                .map(a -> new AccountPrincipal(a.getId(), a.getEmail(), a.fullName(), a.getRole()));
    }

    @Transactional
    public void logout(String token) {
        if (Tokens.isHexToken(token)) {
            sessions.deleteById(Tokens.sha256Hex(token));
        }
    }

    @Transactional(readOnly = true)
    public AccountResponse me(UUID accountId) {
        return view(accounts.findById(accountId).orElseThrow(() -> ApiException.unauthorized("Accesso non effettuato.")));
    }

    // ───────────── Password dimenticata ─────────────

    /** Risposta sempre uguale: non si rivela se l'email è registrata. */
    @Transactional
    public Outcome forgotPassword(String emailInput) {
        String email = Texts.clean(emailInput).toLowerCase();
        return accounts.findByEmail(email).filter(a -> a.getStatus() != AccountStatus.suspended).map(a -> {
            String raw = newToken(a, TokenPurpose.reset_password, RESET_TTL);
            return new Outcome(mailer.passwordReset(a.getEmail(), a.getFirstName(), raw), true);
        }).orElse(new Outcome("not_required", false));
    }

    /** Imposta la nuova password, chiude tutte le sessioni aperte e, se serviva, conferma anche l'email. */
    @Transactional
    public void resetPassword(AccountRequests.ResetPassword r) {
        AccountToken token = usableToken(r.token(), TokenPurpose.reset_password);
        UserAccount account = accounts.findById(token.getAccountId())
                .orElseThrow(() -> ApiException.badRequest("Il collegamento non è valido o è scaduto."));
        PasswordPolicy.check(r.password(), account.getEmail());
        account.setPasswordHash(encoder.encode(r.password()));
        if (account.getStatus() == AccountStatus.pending_verification) {
            account.setStatus(AccountStatus.active); // ha dimostrato di controllare la casella
            account.setEmailVerifiedAt(clock.instant());
        }
        token.setUsedAt(clock.instant());
        sessions.deleteByAccountId(account.getId());
    }

    // ───────────── Helper ─────────────

    /** Controlli comuni di registrazione: restituisce l'email normalizzata. */
    public static String checkIdentity(String first, String last, String emailInput, String password, Boolean acceptTerms) {
        String f = Texts.clean(first);
        String l = Texts.clean(last);
        String email = Texts.clean(emailInput).toLowerCase();
        if (f.isEmpty() || f.length() > 80 || l.isEmpty() || l.length() > 80) {
            throw ApiException.badRequest("Inserisci nome e cognome (massimo 80 caratteri ciascuno).");
        }
        if (email.length() > 254 || !Texts.EMAIL.matcher(email).matches()) {
            throw ApiException.badRequest("Inserisci un indirizzo email valido.");
        }
        PasswordPolicy.check(password, email);
        if (!Boolean.TRUE.equals(acceptTerms)) {
            throw ApiException.badRequest("Per registrarti devi accettare le condizioni e l’informativa sulla privacy.");
        }
        return email;
    }

    private String sendVerification(UserAccount account) {
        String raw = newToken(account, TokenPurpose.verify_email, VERIFY_TTL);
        return mailer.verification(account.getEmail(), account.getFirstName(), raw);
    }

    private String newToken(UserAccount account, TokenPurpose purpose, Duration ttl) {
        tokens.deleteUnused(account.getId(), purpose);
        String raw = Tokens.randomToken();
        AccountToken token = new AccountToken();
        token.setTokenHash(Tokens.sha256Hex(raw));
        token.setAccountId(account.getId());
        token.setPurpose(purpose);
        token.setExpiresAt(clock.instant().plus(ttl));
        tokens.save(token);
        return raw;
    }

    private AccountToken usableToken(String raw, TokenPurpose purpose) {
        if (!Tokens.isHexToken(raw)) {
            throw ApiException.badRequest("Il collegamento non è valido o è scaduto.");
        }
        return tokens.findByTokenHashAndPurpose(Tokens.sha256Hex(raw), purpose)
                .filter(t -> t.getUsedAt() == null && t.getExpiresAt().isAfter(clock.instant()))
                .orElseThrow(() -> ApiException.badRequest("Il collegamento non è valido o è scaduto."));
    }

    AccountResponse view(UserAccount a) {
        AccountResponse.BusinessSummary business = a.getRole() == AccountRole.merchant
                ? companies.findByOwnerAccountId(a.getId()).map(AccountService::summary).orElse(null) : null;
        return new AccountResponse(a.getId(), a.getEmail(), a.getFirstName(), a.getLastName(), a.fullName(),
                a.getRole(), a.getStatus(), business);
    }

    static AccountResponse.BusinessSummary summary(CompanyProfile c) {
        return new AccountResponse.BusinessSummary(c.getId(), c.getBusinessName(), c.getRegistrationStatus().name(),
                c.getStatus().name(), c.getReviewNote(), c.isPublished());
    }
}
