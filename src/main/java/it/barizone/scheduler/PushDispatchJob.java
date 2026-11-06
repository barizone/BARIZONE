package it.barizone.scheduler;

import java.time.Instant;

import it.barizone.dto.response.SendResult;
import it.barizone.model.entity.PushCampaign;
import it.barizone.model.enums.CampaignStatus;
import it.barizone.repository.AdminSessionRepository;
import it.barizone.repository.MerchantDeliveryRepository;
import it.barizone.repository.MerchantMessageRepository;
import it.barizone.repository.MerchantSessionRepository;
import it.barizone.repository.PushCampaignRepository;
import it.barizone.service.GroupPushDispatcher;
import it.barizone.service.PushCampaignDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Novità rispetto alla versione Worker: gli invii avviati proseguono da soli.
 * Prima l'invio avanzava solo finché la pagina admin restava aperta e richiamava l'API.
 */
@Component
@ConditionalOnProperty(prefix = "barizone.jobs", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PushDispatchJob {

    private static final Logger log = LoggerFactory.getLogger(PushDispatchJob.class);
    private static final int MAX_BATCHES_PER_RUN = 40;

    private final PushCampaignRepository campaigns;
    private final PushCampaignDispatcher campaignDispatcher;
    private final MerchantDeliveryRepository merchantDeliveries;
    private final MerchantMessageRepository merchantMessages;
    private final GroupPushDispatcher groupDispatcher;
    private final AdminSessionRepository adminSessions;
    private final MerchantSessionRepository merchantSessions;

    public PushDispatchJob(PushCampaignRepository campaigns, PushCampaignDispatcher campaignDispatcher,
                           MerchantDeliveryRepository merchantDeliveries, MerchantMessageRepository merchantMessages,
                           GroupPushDispatcher groupDispatcher, AdminSessionRepository adminSessions,
                           MerchantSessionRepository merchantSessions) {
        this.campaigns = campaigns;
        this.campaignDispatcher = campaignDispatcher;
        this.merchantDeliveries = merchantDeliveries;
        this.merchantMessages = merchantMessages;
        this.groupDispatcher = groupDispatcher;
        this.adminSessions = adminSessions;
        this.merchantSessions = merchantSessions;
    }

    @Scheduled(fixedDelayString = "${barizone.push.dispatch-interval-ms:30000}", initialDelay = 30_000)
    public void dispatch() {
        for (PushCampaign c : campaigns.findByStatus(CampaignStatus.sending)) {
            int batches = 0;
            SendResult r;
            do {
                r = campaignDispatcher.processBatch(c.getId());
            } while (r.more() && ++batches < MAX_BATCHES_PER_RUN);
        }
        for (var messageId : merchantDeliveries.messagesWithPending()) {
            merchantMessages.findById(messageId).ifPresent(m -> {
                int batches = 0;
                while (Boolean.TRUE.equals(groupDispatcher.processBatch(m).get("more")) && ++batches < MAX_BATCHES_PER_RUN) {
                    log.debug("Comunicazione {}: lotto {}", m.getId(), batches);
                }
            });
        }
    }

    /** Pulizia oraria delle sessioni scadute. */
    @Scheduled(cron = "0 17 * * * *")
    public void cleanup() {
        Instant now = Instant.now();
        adminSessions.deleteExpired(now);
        merchantSessions.deleteExpired(now);
    }
}
