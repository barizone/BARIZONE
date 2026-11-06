package it.barizone.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Arrays;

import it.barizone.util.Tokens;
import nl.martijndwars.webpush.Encoding;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.client.methods.HttpPost;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Test;

/**
 * Verifica che le chiavi nel formato della versione Worker (pubblica raw 65 byte, privata = scalare d)
 * siano accettate dalla libreria Java e producano una richiesta Web Push firmata e cifrata.
 * Nessuna rete: si prepara la richiesta senza inviarla.
 */
class WebPushEncryptionTest {

    @Test
    void preparesSignedEncryptedRequestWithWorkerKeyFormat() throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        KeyPair server = p256();
        KeyPair browser = p256();
        PushService service = new PushService(Tokens.base64Url(raw((ECPublicKey) server.getPublic())),
                Tokens.base64Url(fixed32(((ECPrivateKey) server.getPrivate()).getS())), "mailto:info@barizone.it");
        Notification n = new Notification("https://fcm.googleapis.com/fcm/send/abc",
                Tokens.base64Url(raw((ECPublicKey) browser.getPublic())), Tokens.base64Url(new byte[16]),
                "{\"title\":\"Barizone\"}".getBytes(), 3600);

        HttpPost post = service.preparePost(n, Encoding.AES128GCM);

        assertThat(post.getFirstHeader("Authorization").getValue()).startsWith("vapid t=");
        assertThat(post.getFirstHeader("Content-Encoding").getValue()).isEqualTo("aes128gcm");
        assertThat(post.getFirstHeader("TTL").getValue()).isEqualTo("3600");
        assertThat(post.getEntity().getContentLength()).isGreaterThan(86);
    }

    private static KeyPair p256() throws Exception {
        KeyPairGenerator g = KeyPairGenerator.getInstance("EC");
        g.initialize(new ECGenParameterSpec("secp256r1"));
        return g.generateKeyPair();
    }

    private static byte[] raw(ECPublicKey key) {
        byte[] out = new byte[65];
        out[0] = 4;
        System.arraycopy(fixed32(key.getW().getAffineX()), 0, out, 1, 32);
        System.arraycopy(fixed32(key.getW().getAffineY()), 0, out, 33, 32);
        return out;
    }

    private static byte[] fixed32(BigInteger v) {
        byte[] b = v.toByteArray();
        byte[] out = new byte[32];
        int n = Math.min(32, b.length);
        System.arraycopy(b, b.length - n, out, 32 - n, n);
        return out;
    }
}
