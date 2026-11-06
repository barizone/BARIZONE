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

    @BeforeEach
    void cleanDatabase() throws Exception {
        jdbc.execute("""
                TRUNCATE news, company_profiles, business_spotlight, media_files, app_config, admin_sessions,
                         login_attempts, news_subscribers, news_subscriber_consents, push_subscriptions,
                         push_campaigns, push_deliveries, merchant_sessions, merchant_clients, merchant_memberships,
                         merchant_messages, merchant_deliveries, merchant_device_subscriptions CASCADE""");
        push.reset();
        email.reset();
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
}
