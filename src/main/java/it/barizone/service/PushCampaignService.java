package it.barizone.service;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import it.barizone.dto.request.CampaignRequest;
import it.barizone.dto.request.ConfirmRequest;
import it.barizone.dto.request.EndpointRequest;
import it.barizone.dto.response.CampaignResponse;
import it.barizone.dto.response.SendResult;
import it.barizone.exception.ApiException;
import it.barizone.integration.PushMessage;
import it.barizone.integration.PushTarget;
import it.barizone.integration.WebPushGateway;
import it.barizone.model.entity.PushCampaign;
import it.barizone.model.entity.PushSubscription;
import it.barizone.model.enums.CampaignKind;
import it.barizone.model.enums.CampaignStatus;
import it.barizone.model.enums.DeliveryStatus;
import it.barizone.model.enums.District;
import it.barizone.model.enums.PaymentStatus;
import it.barizone.repository.PushCampaignRepository;
import it.barizone.repository.PushDeliveryRepository;
import it.barizone.repository.PushSubscriptionRepository;
import it.barizone.util.Texts;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Campagne push gestite dall'admin. Regole commerciali invariate: una campagna sponsorizzata
 * si invia solo con committente, riferimento dell'accordo, importo e pagamento confermato,
 * e sempre con una conferma esplicita.
 */
@Service
public class PushCampaignService {

    private final PushCampaignRepository campaigns;
    private final PushSubscriptionRepository subscriptions;
    private final PushDeliveryRepository deliveries;
    private final PushCampaignDispatcher dispatcher;
    private final WebPushGateway gateway;

    public PushCampaignService(PushCampaignRepository campaigns, PushSubscriptionRepository subscriptions,
                               PushDeliveryRepository deliveries, PushCampaignDispatcher dispatcher,
                               WebPushGateway gateway) {
        this.campaigns = campaigns;
        this.subscriptions = subscriptions;
        this.deliveries = deliveries;
        this.dispatcher = dispatcher;
        this.gateway = gateway;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> overview() {
        List<CampaignResponse> list = campaigns.findAllByOrderByCreatedAtDesc(Limit.of(100)).stream()
                .map(c -> CampaignResponse.of(c,
                        deliveries.countByCampaignIdAndStatus(c.getId(), DeliveryStatus.accepted),
                        deliveries.countByCampaignIdAndStatusNot(c.getId(), DeliveryStatus.accepted)))
                .toList();
        List<Map<String, Object>> groups = subscriptions.countByDistrict().stream()
                .map(g -> Map.<String, Object>of("quartiere", g.getQuartiere(), "total", g.getTotal())).toList();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("campaigns", list);
        result.put("groups", groups);
        result.put("total", groups.stream().mapToLong(g -> (Long) g.get("total")).sum());
        return result;
    }

    @Transactional
    public UUID create(CampaignRequest body, String origin, String createdBy) {
        PushCampaign c = new PushCampaign();
        c.setId(UUID.randomUUID());
        c.setCreatedAt(Instant.now());
        c.setCreatedBy(createdBy);
        c.setStatus(CampaignStatus.draft);
        apply(c, body, origin);
        campaigns.save(c);
        return c.getId();
    }

    @Transactional
    public UUID update(UUID id, CampaignRequest body, String origin) {
        PushCampaign c = campaigns.findById(id).filter(x -> x.getStatus() == CampaignStatus.draft)
                .orElseThrow(() -> ApiException.badRequest("Solo le bozze possono essere modificate"));
        apply(c, body, origin);
        return id;
    }

    /** Invia un lotto e restituisce l'avanzamento. Il resto prosegue anche in background (PushDispatchJob). */
    public SendResult send(UUID id, ConfirmRequest body) {
        PushCampaign c = campaigns.findById(id).orElseThrow(() -> ApiException.notFound("Campagna non trovata"));
        if (!c.readyToSend()) {
            throw ApiException.badRequest("Per inviare una campagna commerciale occorrono attività committente, "
                    + "riferimento dell’accordo, importo e pagamento confermato.");
        }
        if (!Boolean.TRUE.equals(body.confirm())) {
            throw ApiException.badRequest("Conferma esplicita richiesta per l’invio");
        }
        dispatcher.start(id);
        return dispatcher.processBatch(id);
    }

    /** Prova sul solo dispositivo dell'admin. */
    @Transactional
    public Map<String, Integer> test(EndpointRequest body) {
        String endpoint = PushSubscriptionService.endpoint(body);
        PushSubscription sub = subscriptions.findById(PushSubscriptionService.idFor(endpoint))
                .orElseThrow(() -> ApiException.notFound("Attiva prima le notifiche su questo dispositivo."));
        PushMessage message = new PushMessage("Barizone · notifica di prova",
                "Questo è un test riservato all’amministratore.", "/notizie", null);
        if (body.campaignId() != null) {
            UUID campaignId;
            try {
                campaignId = UUID.fromString(String.valueOf(body.campaignId()));
            } catch (IllegalArgumentException e) {
                throw ApiException.notFound("Campagna di prova non trovata.");
            }
            PushCampaign c = campaigns.findById(campaignId)
                    .orElseThrow(() -> ApiException.notFound("Campagna di prova non trovata."));
            message = new PushMessage("[PROVA] " + c.getTitle(), c.getBody(), c.getUrl(), null);
        }
        int status = gateway.send(new PushTarget(sub.getEndpoint(), sub.getP256dh(), sub.getAuth()), message);
        if (WebPushGateway.gone(status)) {
            subscriptions.delete(sub);
        }
        return Map.of("accepted", WebPushGateway.accepted(status) ? 1 : 0, "total", 1);
    }

    private static void apply(PushCampaign c, CampaignRequest b, String origin) {
        String title = Texts.clean(b.title());
        String text = Texts.clean(b.body());
        String quartiere = Texts.clean(b.quartiere());
        URI target;
        try {
            target = URI.create(origin + "/").resolve(Texts.clean(b.url()));
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("Collegamento non valido");
        }
        if (title.isEmpty() || title.length() > 80 || text.isEmpty() || text.length() > 200 || !District.isValid(quartiere)
                || !"https".equals(target.getScheme()) || target.getUserInfo() != null) {
            throw ApiException.badRequest("Inserisci titolo (massimo 80 caratteri), testo (massimo 200), quartiere e collegamento https.");
        }
        CampaignKind kind;
        try {
            kind = b.kind() == null ? CampaignKind.sponsored : CampaignKind.valueOf(String.valueOf(b.kind()));
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("Tipo di comunicazione non valido");
        }
        long amount = kind == CampaignKind.barizone ? 0 : safeInteger(b.amountCents());
        String payment = Texts.clean(b.paymentStatus());
        if (amount < 0 || !(payment.equals("pending") || payment.equals("paid"))) {
            throw ApiException.badRequest("Importo o stato del pagamento non valido");
        }
        c.setKind(kind);
        c.setTitle(title);
        c.setBody(text);
        c.setUrl(target.toString());
        c.setBusiness(kind == CampaignKind.barizone ? "Barizone" : Texts.clean(b.business()));
        c.setContractRef(kind == CampaignKind.barizone ? "" : Texts.clean(b.contractRef()));
        c.setAmountCents(amount);
        c.setPaymentStatus(PaymentStatus.valueOf(payment));
        c.setQuartiere(quartiere);
    }

    /** Equivalente di Number.isSafeInteger: -1 se non è un intero valido. */
    private static long safeInteger(Object value) {
        try {
            BigDecimal d = new BigDecimal(String.valueOf(value).trim());
            return d.stripTrailingZeros().scale() <= 0 && d.abs().compareTo(BigDecimal.valueOf(9007199254740991L)) <= 0
                    ? d.longValueExact() : -1;
        } catch (NumberFormatException | ArithmeticException e) {
            return -1;
        }
    }
}
