package it.barizone.integration;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Map;

import it.barizone.configuration.BarizoneProperties;
import it.barizone.repository.AppConfigRepository;
import it.barizone.util.Tokens;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Chiavi VAPID. Ordine di priorità:
 * <ol>
 *   <li>variabili d'ambiente {@code VAPID_PUBLIC_KEY}/{@code VAPID_PRIVATE_KEY};</li>
 *   <li>riga {@code vapid} in app_config, migrata dalla versione D1 (stesso formato JSON);</li>
 *   <li>generazione al primo uso, salvata in DB.</li>
 * </ol>
 * Cambiare le chiavi invalida tutte le iscrizioni esistenti: per questo vengono migrate, non rigenerate.
 */
@Component
public class VapidKeyProvider {

    private static final String KEY = "vapid";

    private final AppConfigRepository config;
    private final BarizoneProperties.Push properties;
    private final ObjectMapper mapper;
    private volatile VapidKeys cached;

    public VapidKeyProvider(AppConfigRepository config, BarizoneProperties properties, ObjectMapper mapper) {
        this.config = config;
        this.properties = properties.push();
        this.mapper = mapper;
    }

    public VapidKeys keys() {
        VapidKeys current = cached;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (cached == null) {
                cached = load();
            }
            return cached;
        }
    }

    private VapidKeys load() {
        if (notBlank(properties.publicKey()) && notBlank(properties.privateKey())) {
            return new VapidKeys(properties.publicKey(), properties.privateKey(), properties.subject());
        }
        if (config.findById(KEY).isEmpty()) {
            config.insertIfAbsent(KEY, mapper.writeValueAsString(generate()));
        }
        Map<?, ?> stored = mapper.readValue(config.findById(KEY).orElseThrow().getValue(), Map.class);
        String subject = stored.get("subject") instanceof String s ? s : properties.subject();
        return new VapidKeys((String) stored.get("publicKey"), (String) stored.get("privateKey"), subject);
    }

    private Map<String, String> generate() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(new ECGenParameterSpec("secp256r1"));
            KeyPair pair = generator.generateKeyPair();
            ECPublicKey pub = (ECPublicKey) pair.getPublic();
            ECPrivateKey priv = (ECPrivateKey) pair.getPrivate();
            byte[] raw = new byte[65];
            raw[0] = 0x04;
            System.arraycopy(fixed32(pub.getW().getAffineX()), 0, raw, 1, 32);
            System.arraycopy(fixed32(pub.getW().getAffineY()), 0, raw, 33, 32);
            return Map.of("publicKey", Tokens.base64Url(raw), "privateKey", Tokens.base64Url(fixed32(priv.getS())),
                    "subject", properties.subject());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Generazione chiavi VAPID non riuscita", e);
        }
    }

    /** BigInteger → esattamente 32 byte big-endian (senza byte di segno, con zeri iniziali se servono). */
    static byte[] fixed32(BigInteger value) {
        byte[] bytes = value.toByteArray();
        byte[] out = new byte[32];
        int copy = Math.min(bytes.length, 32);
        System.arraycopy(bytes, bytes.length - copy, out, 32 - copy, copy);
        return out;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
