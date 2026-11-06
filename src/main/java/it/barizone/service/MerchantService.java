package it.barizone.service;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import it.barizone.dto.request.CompanyRequest;
import it.barizone.dto.response.MerchantResponse;
import it.barizone.exception.ApiException;
import it.barizone.model.entity.CompanyProfile;
import it.barizone.model.enums.CompanyStatus;
import it.barizone.model.enums.District;
import it.barizone.repository.CompanyProfileRepository;
import it.barizone.util.Texts;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MerchantService {

    public static final Pattern MEDIA_PATH = Pattern.compile("^/api/media/[a-f0-9-]{36}$");
    private static final Set<String> MAPS_HOSTS =
            Set.of("www.google.com", "google.com", "maps.google.com", "maps.app.goo.gl", "goo.gl");

    private final CompanyProfileRepository repository;

    public MerchantService(CompanyProfileRepository repository) {
        this.repository = repository;
    }

    // ───────────── Pubblico ─────────────

    @Transactional(readOnly = true)
    public List<MerchantResponse> publicList(String quartiere) {
        String q = District.canonical(quartiere);
        return repository.findByStatusOrderByUpdatedAtDesc(CompanyStatus.published).stream()
                .map(MerchantResponse::from)
                .filter(m -> q.isEmpty() || m.quartiere().equals(q))
                .toList();
    }

    @Transactional(readOnly = true)
    public MerchantResponse publicDetail(String id) {
        return repository.findByIdAndStatus(id, CompanyStatus.published).map(MerchantResponse::from)
                .orElseThrow(() -> ApiException.notFound("Attività non disponibile."));
    }

    // ───────────── Admin ─────────────

    @Transactional(readOnly = true)
    public List<MerchantResponse> adminList() {
        return repository.findAllByOrderByUpdatedAtDesc().stream()
                .map(c -> MerchantResponse.of(c, publicationIssues(c))).toList();
    }

    @Transactional
    public MerchantResponse create(CompanyRequest request) {
        CompanyProfile profile = new CompanyProfile();
        profile.setId(UUID.randomUUID().toString());
        apply(profile, request);
        profile.setStatus(CompanyStatus.draft);
        profile.setUpdatedAt(Instant.now());
        repository.save(profile);
        return MerchantResponse.of(profile, publicationIssues(profile));
    }

    /** Una scheda pubblicata resta pubblicata solo se ancora completa; altrimenti torna in bozza. */
    @Transactional
    public MerchantResponse update(String id, CompanyRequest request) {
        CompanyProfile profile = repository.findById(id).orElseThrow(() -> ApiException.notFound("Azienda non trovata."));
        boolean wasPublished = profile.isPublished();
        apply(profile, request);
        profile.setStatus(wasPublished && publicationIssues(profile).isEmpty() ? CompanyStatus.published : CompanyStatus.draft);
        profile.setUpdatedAt(Instant.now());
        repository.save(profile);
        return MerchantResponse.of(profile, publicationIssues(profile));
    }

    @Transactional
    public void changeStatus(String id, String status) {
        CompanyProfile profile = repository.findById(id).orElseThrow(() -> ApiException.notFound("Azienda non trovata."));
        CompanyStatus next;
        try {
            next = CompanyStatus.valueOf(status == null ? "" : status);
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("Stato non valido.");
        }
        if (next == CompanyStatus.published) {
            List<String> missing = publicationIssues(profile);
            if (!missing.isEmpty()) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Scheda salvata in bozza. Per pubblicare completa: " + String.join(", ", missing) + ".",
                        Map.of("missing_fields", missing));
            }
        }
        profile.setStatus(next);
        profile.setUpdatedAt(Instant.now());
    }

    /** Campi obbligatori per la pubblicazione. */
    public static List<String> publicationIssues(CompanyProfile c) {
        return c.getBusinessName() == null || c.getBusinessName().isBlank() ? List.of("Nome attività") : List.of();
    }

    private static void apply(CompanyProfile p, CompanyRequest r) {
        String businessName = Texts.clean(r.businessName());
        if (businessName.isEmpty() || businessName.length() > 160) {
            throw ApiException.badRequest("Inserisci il nome dell’attività (massimo 160 caratteri).");
        }
        String[] longFields = {r.description(), r.slogan(), r.promotion()};
        String[] shortFields = {r.ownerName(), r.legalName(), r.vatNumber(), r.taxCode(), r.legalAddress(),
                r.postalCode(), r.city(), r.province(), r.email(), r.pec(), r.phone(), r.category(), r.quartiere(),
                r.address(), r.plan(), r.coverImage(), r.mapsUrl(), r.entityType()};
        for (String f : longFields) {
            if (Texts.clean(f).length() > 3000) {
                throw ApiException.badRequest("Uno dei campi è troppo lungo.");
            }
        }
        for (String f : shortFields) {
            if (Texts.clean(f).length() > 500) {
                throw ApiException.badRequest("Uno dei campi è troppo lungo.");
            }
        }
        String vat = Texts.clean(r.vatNumber());
        if (!vat.isEmpty() && !vat.matches("^\\d{11}$")) {
            throw ApiException.badRequest("La partita IVA deve contenere 11 cifre.");
        }
        String email = Texts.clean(r.email());
        if (!email.isEmpty() && !Texts.EMAIL.matcher(email).matches()) {
            throw ApiException.badRequest("Email non valida.");
        }
        String pec = Texts.clean(r.pec());
        if (!pec.isEmpty() && !Texts.EMAIL.matcher(pec).matches()) {
            throw ApiException.badRequest("PEC non valida.");
        }
        String postal = Texts.clean(r.postalCode());
        if (!postal.isEmpty() && !postal.matches("^\\d{5}$")) {
            throw ApiException.badRequest("Il CAP deve contenere 5 cifre.");
        }
        String cover = Texts.clean(r.coverImage());
        if (!cover.isEmpty() && !MEDIA_PATH.matcher(cover).matches() && !Texts.isHttpsUrl(cover)) {
            throw ApiException.badRequest("Usa un collegamento HTTPS per la foto.");
        }
        String maps = Texts.clean(r.mapsUrl());
        if (!maps.isEmpty()) {
            URI uri = Texts.parseHttpUrl(maps);
            if (uri == null || !"https".equalsIgnoreCase(uri.getScheme()) || !MAPS_HOSTS.contains(uri.getHost())) {
                throw ApiException.badRequest("Inserisci un collegamento Google Maps valido.");
            }
        }
        List<String> photos = new ArrayList<>();
        if (r.photos() != null) {
            for (Object photo : r.photos().subList(0, Math.min(6, r.photos().size()))) {
                if (!(photo instanceof String s) || (!MEDIA_PATH.matcher(s).matches() && !s.startsWith("https://"))) {
                    throw ApiException.badRequest("Foto non valida.");
                }
                photos.add(s);
            }
        }
        p.setBusinessName(businessName);
        p.setOwnerName(Texts.clean(r.ownerName()));
        p.setLegalName(Texts.clean(r.legalName()));
        p.setVatNumber(vat);
        p.setTaxCode(Texts.clean(r.taxCode()));
        p.setLegalAddress(Texts.clean(r.legalAddress()));
        p.setPostalCode(postal);
        p.setCity(Texts.clean(r.city()));
        p.setProvince(Texts.clean(r.province()));
        p.setEmail(email);
        p.setPec(pec);
        p.setPhone(Texts.clean(r.phone()));
        p.setCategory(Texts.clean(r.category()));
        p.setQuartiere(District.canonical(Texts.clean(r.quartiere())));
        p.setAddress(Texts.clean(r.address()));
        p.setDescription(Texts.clean(r.description()));
        p.setSlogan(Texts.clean(r.slogan()));
        p.setPromotion(Texts.clean(r.promotion()));
        p.setPlan(Texts.clean(r.plan()));
        p.setCoverImage(cover);
        p.setMapsUrl(maps);
        p.setEntityType(Texts.clean(r.entityType()));
        p.setPhotos(photos);
        p.setDemo(Boolean.TRUE.equals(r.isDemo()));
        p.setVerified(Boolean.TRUE.equals(r.verified()));
        p.setContractActive(Boolean.TRUE.equals(r.contractActive()));
        p.setNotificationsEnabled(false);
    }
}
