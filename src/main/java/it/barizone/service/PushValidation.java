package it.barizone.service;

import java.net.URI;
import java.util.Map;
import java.util.regex.Pattern;

import it.barizone.integration.PushTarget;

/** Accetta solo iscrizioni verso i servizi push reali dei browser (FCM, Mozilla, Apple, Windows). */
public final class PushValidation {

    private static final Pattern P256DH = Pattern.compile("^[A-Za-z0-9_-]{80,100}$");
    private static final Pattern AUTH = Pattern.compile("^[A-Za-z0-9_-]{20,30}$");

    private PushValidation() {
    }

    public static PushTarget parse(Object subscription) {
        if (!(subscription instanceof Map<?, ?> s) || !(s.get("endpoint") instanceof String endpoint)
                || !(s.get("keys") instanceof Map<?, ?> keys)) {
            return null;
        }
        URI uri;
        try {
            uri = URI.create(endpoint);
        } catch (IllegalArgumentException e) {
            return null;
        }
        String host = uri.getHost();
        boolean trusted = "https".equals(uri.getScheme()) && uri.getPort() == -1 && uri.getUserInfo() == null
                && host != null && (host.equals("fcm.googleapis.com") || host.endsWith(".push.services.mozilla.com")
                || host.endsWith(".push.apple.com") || host.endsWith(".notify.windows.com"));
        String p256dh = keys.get("p256dh") instanceof String p ? p : "";
        String auth = keys.get("auth") instanceof String a ? a : "";
        if (!trusted || !P256DH.matcher(p256dh).matches() || !AUTH.matcher(auth).matches()) {
            return null;
        }
        return new PushTarget(endpoint, p256dh, auth);
    }
}
