package it.barizone.util;

import java.net.URI;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.Instant;
import java.util.regex.Pattern;

/** Helper di validazione con le stesse regole della versione precedente. */
public final class Texts {

    public static final ZoneId ROME = ZoneId.of("Europe/Rome");
    public static final Pattern EMAIL = Pattern.compile("^\\S+@\\S+\\.\\S+$");

    private Texts() {
    }

    /** Stringa ripulita; vuota se il valore manca o non è una stringa. */
    public static String clean(Object value) {
        return value instanceof String s ? s.trim() : "";
    }

    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /** URL assoluto con schema http o https. */
    public static URI parseHttpUrl(String value) {
        try {
            URI uri = URI.create(value);
            if (uri.getScheme() == null || uri.getHost() == null) {
                return null;
            }
            String scheme = uri.getScheme().toLowerCase();
            return scheme.equals("http") || scheme.equals("https") ? uri : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static boolean isHttpsUrl(String value) {
        URI uri = parseHttpUrl(value);
        return uri != null && "https".equalsIgnoreCase(uri.getScheme());
    }

    /** Data di calendario a Bari (Europe/Rome). */
    public static LocalDate romeDate(Instant instant) {
        return instant.atZone(ROME).toLocalDate();
    }
}
