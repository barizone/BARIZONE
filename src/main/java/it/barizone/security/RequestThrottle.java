package it.barizone.security;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import it.barizone.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Limite di richieste per azione e indirizzo IP (in memoria, per istanza).
 * Protegge registrazione, recupero password e reinvio email dall'abuso; il login ha un limite
 * separato, persistito nel database. Dietro più istanze il limite è per istanza: per un limite
 * globale usa anche le regole del proxy (es. Cloudflare).
 */
@Component
public class RequestThrottle {

    private final Cache<String, AtomicInteger> counters =
            Caffeine.newBuilder().expireAfterWrite(Duration.ofHours(1)).maximumSize(50_000).build();

    /** Consente al massimo {@code max} richieste all'ora per ({@code action}, {@code ip}). */
    public void check(String action, String ip, int max) {
        int used = counters.get(action + "|" + (ip == null ? "unknown" : ip), k -> new AtomicInteger()).incrementAndGet();
        if (used > max) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Troppe richieste. Riprova tra un’ora.");
        }
    }

    /**
     * Restituisce una richiesta alla quota: usato quando la richiesta è stata respinta per dati non validi
     * (400) e quindi non ha creato nulla né inviato email. Chi corregge un campo non viene penalizzato.
     */
    public void refund(String action, String ip) {
        AtomicInteger used = counters.getIfPresent(action + "|" + (ip == null ? "unknown" : ip));
        if (used != null) {
            used.decrementAndGet();
        }
    }

    /** Esegue {@code work} contando la richiesta nella quota, a meno che venga respinta come non valida. */
    public <T> T limited(String action, String ip, int max, java.util.function.Supplier<T> work) {
        check(action, ip, max);
        try {
            return work.get();
        } catch (ApiException e) {
            if (e.status() == HttpStatus.BAD_REQUEST) {
                refund(action, ip);
            }
            throw e;
        }
    }

    public void reset() {
        counters.invalidateAll();
    }
}
