package it.barizone.integration;

import java.security.Security;

import nl.martijndwars.webpush.Encoding;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** Web Push standard (RFC 8030/8291/8292): cifratura aes128gcm e firma VAPID, TTL 1 ora. */
@Component
public class VapidWebPushGateway implements WebPushGateway {

    private static final Logger log = LoggerFactory.getLogger(VapidWebPushGateway.class);
    private static final int TTL_SECONDS = 3600;

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private final VapidKeyProvider keys;
    private final ObjectMapper mapper;
    private volatile PushService pushService;

    public VapidWebPushGateway(VapidKeyProvider keys, ObjectMapper mapper) {
        this.keys = keys;
        this.mapper = mapper;
    }

    @Override
    public int send(PushTarget target, PushMessage message) {
        try {
            byte[] payload = mapper.writeValueAsBytes(message);
            Notification notification = new Notification(target.endpoint(), target.p256dh(), target.auth(), payload, TTL_SECONDS);
            HttpResponse response = service().send(notification, Encoding.AES128GCM);
            return response.getStatusLine().getStatusCode();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return 0;
        } catch (Exception e) {
            log.warn("Invio push non riuscito: {}", e.getClass().getSimpleName());
            return 0;
        }
    }

    private PushService service() throws Exception {
        if (pushService == null) {
            synchronized (this) {
                if (pushService == null) {
                    VapidKeys k = keys.keys();
                    pushService = new PushService(k.publicKey(), k.privateKey(), k.subject());
                }
            }
        }
        return pushService;
    }
}
