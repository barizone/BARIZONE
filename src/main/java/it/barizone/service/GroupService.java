package it.barizone.service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import it.barizone.exception.ApiException;
import it.barizone.integration.PushTarget;
import it.barizone.model.entity.CompanyProfile;
import it.barizone.model.entity.MerchantClient;
import it.barizone.model.entity.MerchantDelivery;
import it.barizone.model.entity.MerchantDeviceSubscription;
import it.barizone.model.entity.MerchantMembership;
import it.barizone.model.entity.MerchantMessage;
import it.barizone.model.entity.MerchantSession;
import it.barizone.model.enums.CompanyStatus;
import it.barizone.repository.CompanyProfileRepository;
import it.barizone.repository.MerchantClientRepository;
import it.barizone.repository.MerchantDeliveryRepository;
import it.barizone.repository.MerchantDeviceSubscriptionRepository;
import it.barizone.repository.MerchantMembershipRepository;
import it.barizone.repository.MerchantMessageRepository;
import it.barizone.repository.MerchantSessionRepository;
import it.barizone.security.AdminPrincipal;
import it.barizone.util.Texts;
import it.barizone.util.Tokens;
import org.springframework.data.domain.Limit;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Gruppi esercenti: l'admin dà accesso a un esercente con un link monouso; i clienti aderiscono
 * a un'attività con consenso specifico; l'esercente invia comunicazioni agli aderenti.
 */
@Service
public class GroupService {

    public static final long STAFF_LIFETIME_SECONDS = 12 * 3600L;
    public static final long CLIENT_LIFETIME_SECONDS = 30 * 86400L;

    private final CompanyProfileRepository companies;
    private final MerchantSessionRepository sessions;
    private final MerchantClientRepository clients;
    private final MerchantMembershipRepository memberships;
    private final MerchantMessageRepository messages;
    private final MerchantDeliveryRepository deliveries;
    private final MerchantDeviceSubscriptionRepository devices;
    private final GroupPushDispatcher dispatcher;
    private final TransactionTemplate tx;

    public GroupService(CompanyProfileRepository companies, MerchantSessionRepository sessions,
                        MerchantClientRepository clients, MerchantMembershipRepository memberships,
                        MerchantMessageRepository messages, MerchantDeliveryRepository deliveries,
                        MerchantDeviceSubscriptionRepository devices, GroupPushDispatcher dispatcher,
                        PlatformTransactionManager transactionManager) {
        this.companies = companies;
        this.sessions = sessions;
        this.clients = clients;
        this.memberships = memberships;
        this.messages = messages;
        this.deliveries = deliveries;
        this.devices = devices;
        this.dispatcher = dispatcher;
        this.tx = new TransactionTemplate(transactionManager);
    }

    /** Chi gestisce: l'admin (tutte le attività) o un esercente (solo la propria). */
    public record Staff(String role, String name, String shopId) {
        boolean canManage(String id) {
            return "admin".equals(role) || id.equals(shopId);
        }

    }

    // ───────────── Identità ─────────────

    @Transactional(readOnly = true)
    public Optional<Staff> staff(Optional<AdminPrincipal> admin, Optional<String> merchantToken) {
        if (admin.isPresent()) {
            return Optional.of(new Staff("admin", admin.get().name(), null));
        }
        return merchantToken.filter(Tokens::isHexToken)
                .flatMap(t -> sessions.findByTokenHashAndExpiresAtAfter(Tokens.sha256Hex(t), Instant.now()))
                .map(s -> new Staff("merchant", null, s.getShopId()));
    }

    @Transactional(readOnly = true)
    public Optional<MerchantClient> client(Optional<String> token) {
        return token.filter(Tokens::isHexToken).flatMap(t -> clients.findByTokenHash(Tokens.sha256Hex(t)));
    }

    /** Il link monouso viene scambiato con una sessione nuova: riusarlo non funziona. */
    @Transactional
    public Map.Entry<String, String> merchantLogin(Object token) {
        if (!(token instanceof String t) || !Tokens.isHexToken(t)) {
            throw ApiException.unauthorized("Accesso esercente non valido.");
        }
        String hash = Tokens.sha256Hex(t);
        Instant now = Instant.now();
        MerchantSession session = sessions.findByTokenHashAndExpiresAtAfter(hash, now)
                .orElseThrow(() -> ApiException.unauthorized("Accesso scaduto o già utilizzato. Richiedi un nuovo ingresso a Barizone."));
        String fresh = Tokens.randomToken();
        if (sessions.rotate(hash, Tokens.sha256Hex(fresh), now.plusSeconds(STAFF_LIFETIME_SECONDS), now) == 0) {
            throw ApiException.unauthorized("Collegamento già utilizzato.");
        }
        return Map.entry(session.getShopId(), fresh);
    }

    @Transactional
    public void merchantLogout(Optional<String> token) {
        token.filter(Tokens::isHexToken).ifPresent(t -> sessions.deleteById(Tokens.sha256Hex(t)));
    }

    // ───────────── Stato per admin/esercente ─────────────

    @Transactional(readOnly = true)
    public Map<String, Object> state(Staff user, boolean activityOnly) {
        boolean admin = "admin".equals(user.role());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("user", staffJson(user));
        if (!activityOnly) {
            List<CompanyProfile> shops = admin
                    ? companies.findByStatusOrderByUpdatedAtDesc(CompanyStatus.published)
                    : companies.findByIdAndStatus(user.shopId(), CompanyStatus.published).stream().toList();
            result.put("shops", shops.stream().map(GroupService::shape).toList());
        }
        List<MerchantMembership> members = admin ? memberships.findAllByOrderByUpdatedAtAsc()
                : memberships.findByShopIdOrderByUpdatedAtAsc(user.shopId());
        result.put("clients", members.stream().map(m -> Map.of("id", m.getId(), "shop_id", m.getShopId(),
                "alias", m.getAlias(), "active", m.isActive(), "updated_at", m.getUpdatedAt())).toList());
        List<MerchantMessage> msgs = admin ? messages.findAllByOrderByCreatedAtDesc(Limit.of(300))
                : messages.findByShopIdOrderByCreatedAtDesc(user.shopId(), Limit.of(300));
        Map<UUID, List<MerchantDelivery>> byMessage = deliveries
                .findByIdMessageIdIn(msgs.stream().map(MerchantMessage::getId).toList()).stream()
                .collect(Collectors.groupingBy(d -> d.getId().getMessageId()));
        result.put("messages", msgs.stream().map(m -> {
            List<MerchantDelivery> ds = byMessage.getOrDefault(m.getId(), List.of());
            Map<String, Object> json = new LinkedHashMap<>();
            json.put("id", m.getId());
            json.put("shop_id", m.getShopId());
            json.put("title", m.getTitle());
            json.put("body", m.getBody());
            json.put("created_at", m.getCreatedAt());
            json.put("recipients", ds.stream().map(d -> d.getId().getMembershipId()).toList());
            json.put("accepted", ds.stream().filter(d -> d.getPushStatus() == it.barizone.model.enums.DeliveryStatus.accepted).count());
            json.put("pending", ds.stream().filter(d -> d.getPushStatus() == it.barizone.model.enums.DeliveryStatus.pending).count());
            return json;
        }).toList());
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> shop(String id) {
        return shape(publishedShop(id));
    }

    /** Link di accesso monouso per l'esercente, valido 12 ore. Solo admin. */
    @Transactional
    public Map<String, Object> grantAccess(Staff user, String shopId, String origin) {
        publishedShop(shopId);
        requireManage(user, shopId);
        if (!"admin".equals(user.role())) {
            throw ApiException.forbidden("Solo l’amministratore può assegnare gli accessi.");
        }
        String token = Tokens.randomToken();
        sessions.save(new MerchantSession(Tokens.sha256Hex(token), shopId, Instant.now().plusSeconds(STAFF_LIFETIME_SECONDS)));
        return Map.of("url", origin + "/esercente?accesso=" + token, "expires_hours", 12);
    }

    /** Crea una comunicazione per tutti gli aderenti attivi e invia il primo lotto. */
    public Map<String, Object> sendMessage(Staff user, String shopId, String messageId, Map<String, Object> body) {
        publishedShop(shopId);
        requireManage(user, shopId);
        MerchantMessage message = messageId != null ? existingMessage(shopId, messageId)
                : tx.execute(status -> createMessage(shopId, body));
        return dispatcher.processBatch(message);
    }

    private MerchantMessage existingMessage(String shopId, String messageId) {
        UUID id;
        try {
            id = UUID.fromString(messageId);
        } catch (IllegalArgumentException e) {
            throw ApiException.notFound("Comunicazione non trovata.");
        }
        return messages.findByIdAndShopId(id, shopId).orElseThrow(() -> ApiException.notFound("Comunicazione non trovata."));
    }

    /** Messaggio e consegne in un'unica transazione (vedi {@link #sendMessage}). */
    private MerchantMessage createMessage(String shopId, Map<String, Object> body) {
        String title = text(body, "title", 100);
        String content = text(body, "body", 500);
        if (title.isEmpty() || content.isEmpty()) {
            throw ApiException.badRequest("Inserisci titolo (massimo 100 caratteri) e messaggio (massimo 500).");
        }
        List<MerchantMembership> active = memberships.findByShopIdAndActiveTrue(shopId);
        if (active.isEmpty()) {
            throw ApiException.badRequest("Nessun cliente con adesione attiva per questa attività.");
        }
        MerchantMessage message = new MerchantMessage(UUID.randomUUID(), shopId, title, content, Instant.now());
        messages.save(message);
        for (MerchantMembership m : active) {
            MerchantDelivery d = new MerchantDelivery();
            d.setId(new MerchantDelivery.Key(message.getId(), m.getId()));
            d.setDeliveredAt(message.getCreatedAt());
            deliveries.save(d);
        }
        return message;
    }

    // ───────────── Clienti ─────────────

    /** Adesione a un'attività. Restituisce il nuovo token cliente se è stato creato. */
    @Transactional
    public Optional<String> subscribe(Optional<MerchantClient> current, Map<String, Object> body) {
        String alias = text(body, "alias", 60);
        String shopId = text(body, "shop_id", 64);
        if (alias.isEmpty() || shopId.isEmpty() || !Boolean.TRUE.equals(body.get("consent"))) {
            throw ApiException.badRequest("Inserisci il nome e il consenso specifico a questa attività.");
        }
        if (companies.findByIdAndStatus(shopId, CompanyStatus.published).isEmpty()) {
            throw ApiException.notFound("Attività non disponibile.");
        }
        Instant now = Instant.now();
        String newToken = null;
        MerchantClient person;
        if (current.isPresent()) {
            person = current.get();
        } else {
            newToken = Tokens.randomToken();
            person = clients.save(new MerchantClient(UUID.randomUUID(), Tokens.sha256Hex(newToken), now));
        }
        MerchantMembership m = memberships.findByClientIdAndShopId(person.getId(), shopId).orElseGet(() -> {
            MerchantMembership fresh = new MerchantMembership();
            fresh.setId(UUID.randomUUID());
            return fresh;
        });
        m.setClientId(person.getId());
        m.setShopId(shopId);
        m.setAlias(alias);
        m.setActive(true);
        m.setUpdatedAt(now);
        memberships.save(m);
        return Optional.ofNullable(newToken);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> inbox(Optional<MerchantClient> person) {
        if (person.isEmpty()) {
            return Map.of("memberships", List.of(), "messages", List.of(), "push_active", false);
        }
        UUID clientId = person.get().getId();
        List<MerchantMembership> own = memberships.findByClientId(clientId);
        Map<String, String> names = companies.findAllById(own.stream().map(MerchantMembership::getShopId).toList())
                .stream().collect(Collectors.toMap(CompanyProfile::getId, CompanyProfile::getBusinessName));
        List<Map<String, Object>> ms = own.stream().filter(m -> names.containsKey(m.getShopId()))
                .map(m -> Map.<String, Object>of("id", m.getId(), "shop_id", m.getShopId(), "alias", m.getAlias(),
                        "active", m.isActive(), "shop_name", names.get(m.getShopId())))
                .toList();
        List<Map<String, Object>> inbox = deliveries.inboxFor(clientId).stream().map(r -> {
            Map<String, Object> json = new LinkedHashMap<>();
            json.put("id", r[0]);
            json.put("shop_id", r[1]);
            json.put("title", r[2]);
            json.put("body", r[3]);
            json.put("created_at", r[4]);
            json.put("push_status", r[5]);
            return json;
        }).toList();
        return Map.of("memberships", ms, "messages", inbox, "push_active", devices.existsByClientId(clientId));
    }

    @Transactional
    public void unsubscribe(Optional<MerchantClient> person, Map<String, Object> body) {
        MerchantClient p = person.orElseThrow(() -> ApiException.unauthorized("Iscrizione non trovata su questo dispositivo."));
        memberships.findByClientIdAndShopId(p.getId(), text(body, "shop_id", 64)).ifPresent(m -> {
            m.setActive(false);
            m.setUpdatedAt(Instant.now());
        });
    }

    @Transactional
    public void enablePush(Optional<MerchantClient> person, Map<String, Object> body) {
        MerchantClient p = person.orElseThrow(() -> ApiException.unauthorized("Iscriviti prima a un’attività."));
        PushTarget target = PushValidation.parse(body.get("subscription"));
        if (!Boolean.TRUE.equals(body.get("marketing_consent")) || target == null) {
            throw ApiException.badRequest("Consenso o dispositivo non valido.");
        }
        if (!memberships.existsByClientIdAndActiveTrue(p.getId())) {
            throw ApiException.badRequest("Non hai adesioni attive.");
        }
        String id = Tokens.sha256Hex(target.endpoint());
        devices.deleteByClientIdOrId(p.getId(), id);
        devices.flush();
        MerchantDeviceSubscription s = new MerchantDeviceSubscription();
        s.setId(id);
        s.setClientId(p.getId());
        s.setEndpoint(target.endpoint());
        s.setP256dh(target.p256dh());
        s.setAuth(target.auth());
        s.setConsentedAt(Instant.now());
        devices.save(s);
    }

    @Transactional
    public void disablePush(Optional<MerchantClient> person) {
        MerchantClient p = person.orElseThrow(() -> ApiException.unauthorized("Iscriviti prima a un’attività."));
        devices.deleteByClientId(p.getId());
    }

    // ───────────── Helper ─────────────

    private CompanyProfile publishedShop(String id) {
        return companies.findByIdAndStatus(id, CompanyStatus.published).orElseThrow(() ->
                ApiException.notFound("Attività non disponibile: deve essere pubblicata nella vetrina Barizone."));
    }

    private static void requireManage(Staff user, String shopId) {
        if (user == null || !user.canManage(shopId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Non puoi gestire questa attività.");
        }
    }

    private static Map<String, Object> shape(CompanyProfile c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("status", c.getStatus());
        m.put("name", c.getBusinessName());
        m.put("slogan", c.getSlogan());
        m.put("address", c.getAddress().isEmpty() ? "Bari" : c.getAddress());
        m.put("description", c.getDescription());
        m.put("photo", c.getCoverImage());
        m.put("is_demo", c.isDemo());
        return m;
    }

    /** Stringa ripulita se entro {@code max} caratteri, altrimenti vuota (come la versione Worker). */
    static String text(Map<String, Object> body, String key, int max) {
        String value = Texts.clean(body.get(key));
        return value.length() <= max ? value : "";
    }

    /** Identità in JSON: role + name (admin) oppure role + shop_id (esercente). */
    public static Map<String, Object> staffJson(Staff s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("role", s.role());
        if (s.name() != null) {
            m.put("name", s.name());
        }
        if (s.shopId() != null) {
            m.put("shop_id", s.shopId());
        }
        return m;
    }
}
