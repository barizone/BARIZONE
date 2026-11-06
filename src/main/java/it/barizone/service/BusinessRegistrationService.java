package it.barizone.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import it.barizone.dto.request.AccountRequests;
import it.barizone.dto.response.AccountResponse;
import it.barizone.exception.ApiException;
import it.barizone.model.entity.CompanyProfile;
import it.barizone.model.entity.UserAccount;
import it.barizone.model.enums.AccountRole;
import it.barizone.model.enums.CompanyStatus;
import it.barizone.model.enums.RegistrationStatus;
import it.barizone.repository.CompanyProfileRepository;
import it.barizone.repository.UserAccountRepository;
import it.barizone.service.BusinessValidator.Clean;
import it.barizone.util.Texts;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registrazione delle attività commerciali.
 *
 * <p>Flusso: registrazione (account + scheda con tutti i dati obbligatori) → conferma email →
 * l'admin verifica partita IVA e dati → approvazione = scheda pubblicata. Finché la registrazione
 * è "in attesa" o "rifiutata" la scheda non compare sul sito.
 */
@Service
public class BusinessRegistrationService {

    private final AccountService accountService;
    private final BusinessValidator validator;
    private final CompanyProfileRepository companies;
    private final UserAccountRepository accounts;
    private final AccountMailer mailer;
    private final Clock clock;

    public BusinessRegistrationService(AccountService accountService, BusinessValidator validator,
                                       CompanyProfileRepository companies, UserAccountRepository accounts,
                                       AccountMailer mailer, Clock clock) {
        this.accountService = accountService;
        this.validator = validator;
        this.companies = companies;
        this.accounts = accounts;
        this.mailer = mailer;
        this.clock = clock;
    }

    /** Scheda con i dati del titolare, per l'elenco admin. */
    public record Pending(CompanyProfile company, String accountEmail, String accountName) {
    }

    // ───────────── Registrazione ─────────────

    @Transactional
    public AccountService.Outcome register(AccountRequests.RegisterBusiness r) {
        String email = AccountService.checkIdentity(r.firstName(), r.lastName(), r.email(), r.password(), r.acceptTerms());
        if (!Boolean.TRUE.equals(r.representsBusiness())) {
            throw ApiException.badRequest("Devi dichiarare di essere il titolare o il legale rappresentante dell’attività, o di essere autorizzato a registrarla.");
        }
        Clean data = validator.validate(r.business());
        if (companies.existsByVatNumberAndRegistrationStatusNot(data.vatNumber(), RegistrationStatus.rejected)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Esiste già una registrazione con questa partita IVA. Se è la tua attività, accedi con il tuo account oppure scrivi a info@barizone.it.");
        }
        return accountService.register(email, r.password(), Texts.clean(r.firstName()), Texts.clean(r.lastName()),
                AccountRole.merchant, account -> {
                    CompanyProfile c = new CompanyProfile();
                    c.setId(UUID.randomUUID().toString());
                    apply(c, data);
                    c.setOwnerAccountId(account.getId());
                    c.setStatus(CompanyStatus.draft);
                    c.setRegistrationStatus(RegistrationStatus.pending);
                    c.setRegistrationAt(clock.instant());
                    c.setTermsVersion(account.getTermsVersion());
                    c.setTermsAcceptedAt(account.getTermsAcceptedAt());
                    c.setUpdatedAt(clock.instant());
                    companies.save(c);
                });
    }

    // ───────────── Area attività ─────────────

    @Transactional(readOnly = true)
    public CompanyProfile own(UUID accountId) {
        return companies.findByOwnerAccountId(accountId)
                .orElseThrow(() -> ApiException.notFound("Nessuna attività collegata a questo account."));
    }

    /**
     * Modifica dei dati da parte dell'attività. Cambiare ragione sociale, partita IVA o codice fiscale
     * richiede una nuova verifica: la scheda esce dal sito finché l'admin non la approva di nuovo.
     * Una registrazione rifiutata, una volta corretta, torna "in attesa".
     */
    @Transactional
    public AccountResponse.BusinessSummary updateOwn(UUID accountId, AccountRequests.Business request) {
        CompanyProfile c = companies.findByOwnerAccountId(accountId)
                .orElseThrow(() -> ApiException.notFound("Nessuna attività collegata a questo account."));
        Clean data = validator.validate(request);
        boolean identityChanged = !c.getVatNumber().equals(data.vatNumber()) || !c.getTaxCode().equals(data.taxCode())
                || !c.getLegalName().equals(data.legalName());
        if (identityChanged && !c.getVatNumber().equals(data.vatNumber())
                && companies.existsByVatNumberAndRegistrationStatusNot(data.vatNumber(), RegistrationStatus.rejected)) {
            throw new ApiException(HttpStatus.CONFLICT, "Esiste già una registrazione con questa partita IVA.");
        }
        apply(c, data);
        if (c.getRegistrationStatus() == RegistrationStatus.rejected
                || (c.getRegistrationStatus() == RegistrationStatus.approved && identityChanged)) {
            c.setRegistrationStatus(RegistrationStatus.pending);
            c.setStatus(CompanyStatus.draft);
            c.setRegistrationAt(clock.instant());
            c.setReviewNote("");
        }
        c.setUpdatedAt(clock.instant());
        return AccountService.summary(c);
    }

    // ───────────── Admin ─────────────

    @Transactional(readOnly = true)
    public List<Pending> registrations(RegistrationStatus status) {
        return companies.findByRegistrationStatusOrderByRegistrationAtAsc(status).stream()
                .filter(c -> c.getOwnerAccountId() != null)
                .map(c -> {
                    UserAccount a = accounts.findById(c.getOwnerAccountId()).orElse(null);
                    return new Pending(c, a == null ? "" : a.getEmail(), a == null ? "" : a.fullName());
                }).toList();
    }

    /** Approva: la scheda viene pubblicata e segnata come verificata. */
    @Transactional
    public CompanyProfile approve(String companyId) {
        CompanyProfile c = pending(companyId);
        c.setRegistrationStatus(RegistrationStatus.approved);
        c.setStatus(CompanyStatus.published);
        c.setVerified(true);
        c.setReviewedAt(clock.instant());
        c.setReviewNote("");
        c.setUpdatedAt(clock.instant());
        notifyOwner(c, true, "");
        return c;
    }

    /** Rifiuta con un motivo obbligatorio, mostrato al titolare, che può correggere e reinviare. */
    @Transactional
    public CompanyProfile reject(String companyId, String note) {
        String reason = Texts.clean(note);
        if (reason.isEmpty() || reason.length() > 500) {
            throw ApiException.badRequest("Scrivi il motivo del rifiuto (massimo 500 caratteri): lo vedrà il titolare.");
        }
        CompanyProfile c = pending(companyId);
        c.setRegistrationStatus(RegistrationStatus.rejected);
        c.setStatus(CompanyStatus.draft);
        c.setReviewedAt(clock.instant());
        c.setReviewNote(reason);
        c.setUpdatedAt(clock.instant());
        notifyOwner(c, false, reason);
        return c;
    }

    private CompanyProfile pending(String id) {
        CompanyProfile c = companies.findById(id).orElseThrow(() -> ApiException.notFound("Registrazione non trovata."));
        if (c.getOwnerAccountId() == null || c.getRegistrationStatus() != RegistrationStatus.pending) {
            throw ApiException.conflict("Questa registrazione non è in attesa di verifica.");
        }
        return c;
    }

    private void notifyOwner(CompanyProfile c, boolean approved, String note) {
        accounts.findById(c.getOwnerAccountId()).ifPresent(a -> {
            if (approved) {
                mailer.registrationApproved(a.getEmail(), a.getFirstName(), c.getBusinessName());
            } else {
                mailer.registrationRejected(a.getEmail(), a.getFirstName(), c.getBusinessName(), note);
            }
        });
    }

    private static void apply(CompanyProfile c, Clean d) {
        c.setBusinessName(d.businessName());
        c.setLegalName(d.legalName());
        c.setOwnerName(d.ownerName());
        c.setVatNumber(d.vatNumber());
        c.setTaxCode(d.taxCode());
        c.setLegalAddress(d.legalAddress());
        c.setPostalCode(d.postalCode());
        c.setCity(d.city());
        c.setProvince(d.province());
        c.setPec(d.pec());
        c.setEmail(d.email());
        c.setPhone(d.phone());
        c.setCategory(d.category());
        c.setQuartiere(d.quartiere());
        c.setAddress(d.address());
        c.setDescription(d.description());
        c.setSlogan(d.slogan());
        c.setPromotion(d.promotion());
        c.setMapsUrl(d.mapsUrl());
        c.setCoverImage(d.coverImage());
        c.setEntityType(d.entityType());
        if (c.getPlan() == null || c.getPlan().isBlank()) {
            c.setPlan("Gratuito");
        }
    }

}
