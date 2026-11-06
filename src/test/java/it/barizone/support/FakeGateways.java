package it.barizone.support;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import it.barizone.integration.EmailGateway;
import it.barizone.integration.PushMessage;
import it.barizone.integration.PushTarget;
import it.barizone.integration.WebPushGateway;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Servizi esterni simulati: nessuna email o notifica reale parte durante i test. */
@TestConfiguration
public class FakeGateways {

    public static class FakePush implements WebPushGateway {
        public final List<PushTarget> targets = new CopyOnWriteArrayList<>();
        public final List<PushMessage> messages = new CopyOnWriteArrayList<>();
        public final AtomicInteger status = new AtomicInteger(201);

        @Override
        public int send(PushTarget target, PushMessage message) {
            targets.add(target);
            messages.add(message);
            return status.get();
        }

        public void reset() {
            targets.clear();
            messages.clear();
            status.set(201);
        }
    }

    public static class FakeEmail implements EmailGateway {
        public final List<List<Email>> batches = new ArrayList<>();
        public final List<String> keys = new ArrayList<>();
        public String result = "accepted";

        @Override
        public synchronized String sendBatch(List<Email> emails, String idempotencyKey) {
            batches.add(emails);
            keys.add(idempotencyKey);
            return result;
        }

        @Override
        public boolean enabled() {
            return true;
        }

        public synchronized void reset() {
            batches.clear();
            keys.clear();
            result = "accepted";
        }
    }

    @Bean
    @Primary
    FakePush fakePush() {
        return new FakePush();
    }

    @Bean
    @Primary
    FakeEmail fakeEmail() {
        return new FakeEmail();
    }
}
