package it.barizone.integration;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import it.barizone.configuration.BarizoneProperties;
import it.barizone.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Meteo di Bari da Open-Meteo, con cache di 15 minuti condivisa da tutti i visitatori:
 * una richiesta esterna ogni 15 minuti invece di una per visitatore.
 * Se Open-Meteo non risponde, si serve l'ultimo dato valido (al massimo di 6 ore).
 */
@Component
public class WeatherClient {

    private static final Logger log = LoggerFactory.getLogger(WeatherClient.class);
    private static final Duration FRESH = Duration.ofMinutes(15);
    private static final Duration STALE_LIMIT = Duration.ofHours(6);

    private record Cached(Map<String, Object> data, Instant fetchedAt) {
    }

    private final RestClient client;
    private final String url;
    private final Clock clock;
    private final AtomicReference<Cached> cache = new AtomicReference<>();

    public WeatherClient(RestClient.Builder builder, BarizoneProperties properties, Clock clock) {
        this.client = builder.build();
        this.url = properties.weather().url();
        this.clock = clock;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> forecast() {
        Instant now = clock.instant();
        Cached current = cache.get();
        if (current != null && current.fetchedAt().plus(FRESH).isAfter(now)) {
            return current.data();
        }
        try {
            Map<String, Object> data = client.get().uri(url).retrieve().body(Map.class);
            cache.set(new Cached(data, now));
            return data;
        } catch (RestClientException e) {
            log.warn("Open-Meteo non disponibile: {}", e.getMessage());
            if (current != null && current.fetchedAt().plus(STALE_LIMIT).isAfter(now)) {
                return current.data();
            }
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Meteo temporaneamente non disponibile.");
        }
    }
}
