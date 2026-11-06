package it.barizone.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import it.barizone.dto.response.MerchantResponse;
import it.barizone.dto.response.SpotlightResponse;
import it.barizone.model.entity.BusinessSpotlight;
import it.barizone.model.entity.CompanyProfile;
import it.barizone.model.enums.CompanyStatus;
import it.barizone.repository.BusinessSpotlightRepository;
import it.barizone.repository.CompanyProfileRepository;
import it.barizone.util.Texts;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Azienda in vetrina: una al giorno (calendario di Bari), a rotazione tra quelle pubblicate.
 * La scelta del giorno è salvata, quindi ricaricare la pagina non fa avanzare la coda.
 */
@Service
public class SpotlightService {

    private static final LocalDate ROTATION_START = LocalDate.of(2026, 10, 22);

    private final CompanyProfileRepository companies;
    private final BusinessSpotlightRepository spotlights;
    private final Clock clock;

    public SpotlightService(CompanyProfileRepository companies, BusinessSpotlightRepository spotlights, Clock clock) {
        this.companies = companies;
        this.spotlights = spotlights;
        this.clock = clock;
    }

    @Transactional
    public SpotlightResponse today() {
        LocalDate day = Texts.romeDate(clock.instant());
        List<CompanyProfile> eligible = companies.findByStatusOrderByIdAsc(CompanyStatus.published).stream()
                .sorted(Comparator.comparing(CompanyProfile::getId)).toList();
        if (eligible.isEmpty()) {
            return new SpotlightResponse(day, 1, 0, null);
        }
        long elapsed = ChronoUnit.DAYS.between(ROTATION_START, day);
        CompanyProfile chosen = eligible.get((int) Math.floorMod(elapsed, eligible.size()));

        Optional<BusinessSpotlight> saved = spotlights.findById(day);
        if (saved.isPresent()) {
            Optional<CompanyProfile> still = find(eligible, saved.get().getMerchantId());
            if (still.isPresent()) {
                return new SpotlightResponse(day, 1, eligible.size(), MerchantResponse.from(still.get()));
            }
        } else {
            // Prosegue la rotazione dal giorno precedente registrato, anche se l'elenco è cambiato.
            Optional<BusinessSpotlight> previous = spotlights.findFirstBySpotlightDateBeforeOrderBySpotlightDateDesc(day);
            if (previous.isPresent()) {
                int last = indexOf(eligible, previous.get().getMerchantId());
                if (last >= 0) {
                    chosen = eligible.get((last + 1) % eligible.size());
                }
            }
        }
        spotlights.upsert(day, chosen.getId(), saved.map(BusinessSpotlight::getMerchantId).orElse(""));
        spotlights.flush();
        String fixedId = spotlights.findById(day).map(BusinessSpotlight::getMerchantId).orElse(chosen.getId());
        MerchantResponse merchant = find(eligible, fixedId).map(MerchantResponse::from).orElse(null);
        return new SpotlightResponse(day, 1, eligible.size(), merchant);
    }

    private static Optional<CompanyProfile> find(List<CompanyProfile> list, String id) {
        return list.stream().filter(c -> c.getId().equals(id)).findFirst();
    }

    private static int indexOf(List<CompanyProfile> list, String id) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId().equals(id)) {
                return i;
            }
        }
        return -1;
    }
}
