package it.barizone.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.regex.Pattern;

/**
 * Primitive crittografiche condivise. Gli algoritmi coincidono con quelli della versione Worker
 * (token casuali esadecimali da 32 byte, SHA-256 esadecimale o base64url), così i dati migrati restano validi.
 */
public final class Tokens {

    public static final Pattern HEX_TOKEN = Pattern.compile("^[a-f0-9]{64}$");
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final HexFormat HEX = HexFormat.of();

    private Tokens() {
    }

    /** 32 byte casuali in esadecimale (64 caratteri). */
    public static String randomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return HEX.formatHex(bytes);
    }

    public static boolean isHexToken(String value) {
        return value != null && HEX_TOKEN.matcher(value).matches();
    }

    public static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String sha256Hex(String value) {
        return HEX.formatHex(sha256(value));
    }

    public static String sha256Base64Url(String value) {
        return base64Url(sha256(value));
    }

    public static String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static byte[] fromBase64Url(String value) {
        return Base64.getUrlDecoder().decode(value);
    }

    /** Confronto a tempo costante, anche con lunghezze diverse. */
    public static boolean constantTimeEquals(String left, String right) {
        return MessageDigest.isEqual(
                left.getBytes(StandardCharsets.UTF_8), right.getBytes(StandardCharsets.UTF_8));
    }
}
