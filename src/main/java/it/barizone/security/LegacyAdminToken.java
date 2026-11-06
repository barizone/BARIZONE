package it.barizone.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import it.barizone.util.Tokens;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Token JWT HS256 per l'accesso di riserva con sola password ({@code ADMIN_PASSWORD} + {@code SESSION_SECRET}).
 * Compatibile con i cookie emessi dalla versione Worker.
 */
@Component
public class LegacyAdminToken {

    private final ObjectMapper mapper;

    public LegacyAdminToken(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public String create(String secret, long lifetimeSeconds) {
        long now = Instant.now().getEpochSecond();
        String header = encode(Map.of("alg", "HS256", "typ", "JWT"));
        String payload = encode(Map.of("sub", "admin", "iat", now, "exp", now + lifetimeSeconds));
        String input = header + "." + payload;
        return input + "." + Tokens.base64Url(hmac(secret, input));
    }

    public boolean verify(String secret, String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return false;
        }
        try {
            byte[] expected = hmac(secret, parts[0] + "." + parts[1]);
            if (!MessageDigest.isEqual(expected, Tokens.fromBase64Url(parts[2]))) {
                return false;
            }
            Map<?, ?> header = mapper.readValue(Tokens.fromBase64Url(parts[0]), Map.class);
            Map<?, ?> claims = mapper.readValue(Tokens.fromBase64Url(parts[1]), Map.class);
            return "HS256".equals(header.get("alg")) && "admin".equals(claims.get("sub"))
                    && claims.get("exp") instanceof Number exp && exp.longValue() > Instant.now().getEpochSecond();
        } catch (RuntimeException e) {
            return false;
        }
    }

    private String encode(Map<String, Object> value) {
        return Tokens.base64Url(mapper.writeValueAsBytes(value));
    }

    private static byte[] hmac(String secret, String input) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
