package it.barizone.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import it.barizone.service.EditorialSeedService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Base dei test di integrazione: applicazione completa + PostgreSQL reale (database barizone_test),
 * orologio fissato al 6 novembre 2026, servizi esterni simulati.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({FakeGateways.class, IntegrationTest.FixedClock.class})
public abstract class IntegrationTest {

    public static final String ORIGIN = "http://localhost";
    public static final Instant NOW = Instant.parse("2026-11-06T12:00:00Z");

    public static class FixedClock {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }

    @Autowired
    protected MockMvc mvc;
    @Autowired
    protected JdbcTemplate jdbc;
    @Autowired
    protected FakeGateways.FakePush push;
    @Autowired
    protected FakeGateways.FakeEmail email;
    @Autowired
    protected EditorialSeedService seeder;
    @Autowired
    protected it.barizone.security.RequestThrottle throttle;

    @BeforeEach
    void cleanDatabase() throws Exception {
        jdbc.execute("""
                TRUNCATE news, company_profiles, business_spotlight, media_files, app_config, admin_sessions,
                         login_attempts, user_accounts, account_sessions, account_tokens, news_subscribers, news_subscriber_consents, push_subscriptions,
                         push_campaigns, push_deliveries, merchant_clients, merchant_memberships,
                         merchant_messages, merchant_deliveries, merchant_device_subscriptions CASCADE""");
        push.reset();
        email.reset();
        throttle.reset();
    }

    /** Richiesta di modifica dallo stesso origin (come il frontend). */
    protected static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, String body) {
        return builder.header("Origin", ORIGIN).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    /** Accesso admin con la password di servizio: restituisce il cookie di sessione. */
    protected Cookie adminCookie() throws Exception {
        MvcResult r = mvc.perform(json(post("/api/auth/login"), "{\"password\":\"password-di-servizio\"}")).andReturn();
        return cookie(r, "__Host-barizone_session");
    }

    protected static Cookie cookie(MvcResult result, String name) {
        for (String header : result.getResponse().getHeaders("Set-Cookie")) {
            Matcher m = Pattern.compile(Pattern.quote(name) + "=([^;]*)").matcher(header);
            if (m.find()) {
                return new Cookie(name, m.group(1));
            }
        }
        return null;
    }

    /** Sottoscrizione push valida (endpoint FCM, chiavi della lunghezza prevista). */
    protected static String subscriptionJson(String suffix) {
        String p256dh = ("B" + "x".repeat(86) + suffix).substring(0, 87);
        return "{\"endpoint\":\"https://fcm.googleapis.com/fcm/send/" + suffix + "\",\"keys\":{\"p256dh\":\""
                + p256dh + "\",\"auth\":\"" + "a".repeat(22) + "\"}}";
    }

    // ───────────── Helper per gli account ─────────────

    /** Ultimo link monouso inviato per email (verifica o recupero): il token è nel testo del messaggio. */
    protected String lastEmailToken() {
        for (int i = email.batches.size() - 1; i >= 0; i--) {
            for (var message : email.batches.get(i)) {
                Matcher m = Pattern.compile("token=([a-f0-9]{64})").matcher(message.text());
                if (m.find()) {
                    return m.group(1);
                }
            }
        }
        throw new AssertionError("Nessun link con token nelle email inviate");
    }

    protected static String userJson(String email, String password) {
        return "{\"first_name\":\"Anna\",\"last_name\":\"Verdi\",\"email\":\"" + email + "\",\"password\":\"" + password
                + "\",\"accept_terms\":true}";
    }

    /** Partita IVA formalmente valida: aggiunge la cifra di controllo alle prime 10 cifre. */
    protected static String vat(String first10) {
        int sum = 0;
        for (int i = 0; i < 10; i++) {
            int d = first10.charAt(i) - '0';
            if (i % 2 == 1) {
                d = d * 2 > 9 ? d * 2 - 9 : d * 2;
            }
            sum += d;
        }
        return first10 + (10 - sum % 10) % 10;
    }

    protected static String businessJson(String name, String vatNumber) {
        return "{\"business_name\":\"" + name + "\",\"legal_name\":\"" + name + " S.n.c.\",\"owner_name\":\"Mario Rossi\","
                + "\"vat_number\":\"" + vatNumber + "\",\"tax_code\":\"" + vatNumber + "\",\"legal_address\":\"Via Sparano 10\","
                + "\"postal_code\":\"70121\",\"city\":\"Bari\",\"province\":\"ba\",\"pec\":\"info@pec.example.it\","
                + "\"email\":\"info@example.it\",\"phone\":\"+39 080 1234567\",\"category\":\"Alimentari\",\"quartiere\":\"Murat\","
                + "\"address\":\"Via Sparano 10\",\"description\":\"Panificio artigianale con forno a legna dal 1980.\"}";
    }

    protected static String businessRegistrationJson(String email, String password, String name, String vatNumber) {
        return "{\"first_name\":\"Mario\",\"last_name\":\"Rossi\",\"email\":\"" + email + "\",\"password\":\"" + password
                + "\",\"accept_terms\":true,\"represents_business\":true,\"business\":" + businessJson(name, vatNumber) + "}";
    }

    /** Registra e conferma un utente, poi accede: restituisce il cookie di sessione. */
    protected Cookie userCookie(String email, String password) throws Exception {
        mvc.perform(json(post("/api/account/register"), userJson(email, password)));
        mvc.perform(json(post("/api/account/verify-email"), "{\"token\":\"" + lastEmailToken() + "\"}"));
        MvcResult r = mvc.perform(json(post("/api/account/login"), "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}")).andReturn();
        return cookie(r, "__Host-barizone_account");
    }

    /** Registra un'attività, conferma l'email, la fa approvare dall'admin e accede: restituisce il cookie. */
    protected Cookie approvedMerchantCookie(String email, String password, String name, String vatNumber) throws Exception {
        mvc.perform(json(post("/api/account/register-business"), businessRegistrationJson(email, password, name, vatNumber)));
        mvc.perform(json(post("/api/account/verify-email"), "{\"token\":\"" + lastEmailToken() + "\"}"));
        String id = jdbc.queryForObject("SELECT id FROM company_profiles WHERE vat_number = ?", String.class, vatNumber);
        mvc.perform(post("/api/admin/registrations/" + id + "/approve").header("Origin", ORIGIN).cookie(adminCookie()));
        MvcResult r = mvc.perform(json(post("/api/account/login"), "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}")).andReturn();
        return cookie(r, "__Host-barizone_account");
    }
}
