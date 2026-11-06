package it.barizone.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import it.barizone.configuration.BarizoneProperties;
import it.barizone.integration.PushMessage;
import it.barizone.integration.PushTarget;
import it.barizone.integration.WebPushGateway;
import it.barizone.model.entity.MerchantDelivery;
import it.barizone.model.entity.MerchantDeviceSubscription;
import it.barizone.model.entity.MerchantMembership;
import it.barizone.model.entity.MerchantMessage;
import it.barizone.model.enums.DeliveryStatus;
import it.barizone.repository.MerchantDeliveryRepository;
import it.barizone.repository.MerchantDeviceSubscriptionRepository;
import it.barizone.repository.MerchantMembershipRepository;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;

/** Consegna push delle comunicazioni esercente agli aderenti, a piccoli lotti e senza doppi invii. */
@Component
public class GroupPushDispatcher {

    private final MerchantDeliveryRepository deliveries;
    private final MerchantMembershipRepository memberships;
    private final MerchantDeviceSubscriptionRepository devices;
    private final WebPushGateway gateway;
    private final int batchSize;

    public GroupPushDispatcher(MerchantDeliveryRepository deliveries, MerchantMembershipRepository memberships,
                               MerchantDeviceSubscriptionRepository devices, WebPushGateway gateway,
                               BarizoneProperties properties) {
        this.deliveries = deliveries;
        this.memberships = memberships;
        this.devices = devices;
        this.gateway = gateway;
        this.batchSize = properties.push().groupBatchSize();
    }

    public Map<String, Object> processBatch(MerchantMessage message) {
        List<MerchantDelivery> pending = deliveries.findByIdMessageIdAndPushStatus(
                message.getId(), DeliveryStatus.pending, Limit.of(batchSize));
        PushMessage push = new PushMessage(message.getTitle(), message.getBody(),
                "/adesioni?azienda=" + URLEncoder.encode(message.getShopId(), StandardCharsets.UTF_8),
                message.getId().toString());
        for (MerchantDelivery d : pending) {
            if (deliveries.claim(d.getId()) == 0) {
                continue;
            }
            DeliveryStatus status;
            Optional<MerchantMembership> membership = memberships.findById(d.getId().getMembershipId())
                    .filter(m -> m.isActive() && m.getShopId().equals(message.getShopId()));
            if (membership.isEmpty()) {
                status = DeliveryStatus.revoked;
            } else {
                Optional<MerchantDeviceSubscription> device = devices.findByClientId(membership.get().getClientId());
                if (device.isEmpty()) {
                    status = DeliveryStatus.not_enabled;
                } else {
                    MerchantDeviceSubscription s = device.get();
                    int code = gateway.send(new PushTarget(s.getEndpoint(), s.getP256dh(), s.getAuth()), push);
                    status = WebPushGateway.accepted(code) ? DeliveryStatus.accepted : DeliveryStatus.failed;
                    if (WebPushGateway.gone(code)) {
                        devices.deleteById(s.getId());
                    }
                }
            }
            deliveries.updateStatus(d.getId(), status);
        }
        return summary(message);
    }

    public Map<String, Object> summary(MerchantMessage message) {
        List<MerchantDelivery> all = deliveries.findByIdMessageId(message.getId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", message.getId());
        result.put("recipients", all.size());
        result.put("accepted", count(all, DeliveryStatus.accepted));
        result.put("failed", count(all, DeliveryStatus.failed) + count(all, DeliveryStatus.sending));
        result.put("not_enabled", count(all, DeliveryStatus.not_enabled));
        result.put("more", count(all, DeliveryStatus.pending) > 0);
        return result;
    }

    private static long count(List<MerchantDelivery> all, DeliveryStatus status) {
        return all.stream().filter(d -> d.getPushStatus() == status).count();
    }
}
