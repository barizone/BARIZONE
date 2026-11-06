package it.barizone.security;

import java.security.GeneralSecurityException;
import java.util.HexFormat;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.springframework.stereotype.Component;

/**
 * PBKDF2-HMAC-SHA256, 256 bit, salt esadecimale: identico alla versione Worker (WebCrypto),
 * quindi gli hash già salvati restano validi senza reimpostare le password.
 */
@Component
public class PasswordHasher {

    private static final HexFormat HEX = HexFormat.of();

    public String hash(String password, String saltHex, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), HEX.parseHex(saltHex), iterations, 256);
            byte[] derived = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            spec.clearPassword();
            return HEX.formatHex(derived);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("PBKDF2 non disponibile", e);
        }
    }
}
