package it.barizone.configuration;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configurazione applicativa tipizzata ({@code barizone.*} in application.yml).
 */
@ConfigurationProperties(prefix = "barizone")
public record BarizoneProperties(
        String publicOrigin,
        String allowedOrigins,
        String contactEmail,
        Admin admin,
        Email email,
        Push push,
        Media media,
        Weather weather) {

    public record Admin(String password, String email, String sessionSecret, int sessionHours,
                        int maxLoginAttempts, int loginWindowMinutes) {
        public long sessionSeconds() {
            return sessionHours * 3600L;
        }
    }

    public record Email(boolean enabled, String resendApiKey, String resendBaseUrl, String from) {
        public boolean active() {
            return enabled && resendApiKey != null && !resendApiKey.isBlank();
        }
    }

    public record Push(String subject, String publicKey, String privateKey, int campaignBatchSize,
                       int groupBatchSize, long dispatchIntervalMs) {
    }

    public record Media(String storage) {
    }

    public record Weather(String url) {
    }

    /** Origin pubblico più quelli aggiuntivi configurati. */
    public Set<String> trustedOrigins() {
        Set<String> origins = new LinkedHashSet<>();
        origins.add(stripSlash(publicOrigin));
        if (allowedOrigins != null) {
            Arrays.stream(allowedOrigins.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(BarizoneProperties::stripSlash)
                    .forEach(origins::add);
        }
        return origins;
    }

    private static String stripSlash(String value) {
        return value != null && value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
