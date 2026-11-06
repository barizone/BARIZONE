package it.barizone.integration;

import java.util.List;

/** Invio email transazionali. Esiti: "accepted", "failed" oppure "disabled". */
public interface EmailGateway {

    record Email(String from, List<String> to, String replyTo, String subject, String text) {
    }

    String sendBatch(List<Email> emails, String idempotencyKey);

    boolean enabled();
}
