package it.barizone.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import it.barizone.configuration.BarizoneProperties;
import it.barizone.dto.response.SendResult;
import it.barizone.integration.PushMessage;
import it.barizone.integration.PushTarget;
import it.barizone.integration.WebPushGateway;
import it.barizone.model.entity.PushCampaign;
import it.barizone.model.entity.PushSubscription;
import it.barizone.model.enums.CampaignStatus;
import it.barizone.model.enums.DeliveryStatus;
import it.barizone.repository.PushCampaignRepository;
import it.barizone.repository.PushDeliveryRepository;
import it.barizone.repository.PushSubscriptionRepository;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Invio a lotti di una campagna. Ogni consegna viene prima "prenotata" con un inserimento atomico,
 * quindi più processi (richiesta dell'admin e job in background, o più istanze) non inviano mai
 * due volte allo stesso dispositivo. Le chiamate HTTP avvengono fuori da transazioni lunghe.
 */
@Component
public class PushCampaignDispatcher {

    private final PushCampaignRepository campaigns;
    private final PushSubscriptionRepository subscriptions;
    private final PushDeliveryRepository deliveries;
    private final WebPushGateway gateway;
    private final int batchSize;

    public PushCampaignDispatcher(PushCampaignRepository campaigns, PushSubscriptionRepository subscriptions,
                                  PushDeliveryRepository deliveries, WebPushGateway gateway,
                                  BarizoneProperties properties) {
        this.campaigns = campaigns;
        this.subscriptions = subscriptions;
        this.deliveries = deliveries;
        this.gateway = gateway;
        this.batchSize = properties.push().campaignBatchSize();
    }

    @Transactional
    public void start(UUID id) {
        campaigns.findById(id).ifPresent(c -> {
            if (c.getStartedAt() == null) {
                c.setStartedAt(Instant.now());
            }
            c.setStatus(CampaignStatus.sending);
        });
    }

    public SendResult processBatch(UUID id) {
        PushCampaign c = campaigns.findById(id).orElseThrow();
        List<PushSubscription> batch = subscriptions.pendingForCampaign(c.getStartedAt(), c.getQuartiere(), id, Limit.of(batchSize));
        int accepted = 0;
        int failed = 0;
        PushMessage message = new PushMessage(c.getTitle(), c.getBody(), c.getUrl(), c.getId().toString());
        for (PushSubscription sub : batch) {
            String deliveryId = id + ":" + sub.getId();
            if (deliveries.claim(deliveryId, id, sub.getId(), Instant.now()) == 0) {
                continue;
            }
            int status = gateway.send(new PushTarget(sub.getEndpoint(), sub.getP256dh(), sub.getAuth()), message);
            if (WebPushGateway.accepted(status)) {
                accepted++;
                deliveries.updateStatus(deliveryId, DeliveryStatus.accepted);
            } else {
                failed++;
                deliveries.updateStatus(deliveryId, DeliveryStatus.failed);
                if (WebPushGateway.gone(status)) {
                    subscriptions.deleteById(sub.getId());
                }
            }
        }
        boolean more = !subscriptions.pendingForCampaign(c.getStartedAt(), c.getQuartiere(), id, Limit.of(1)).isEmpty();
        if (!more) {
            complete(id);
        }
        return new SendResult(accepted, failed, more);
    }

    @Transactional
    public void complete(UUID id) {
        campaigns.findById(id).ifPresent(c -> {
            c.setStatus(CampaignStatus.completed);
            campaigns.save(c);
        });
    }
}
