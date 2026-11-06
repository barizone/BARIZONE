package it.barizone.service;

import java.util.List;

import it.barizone.configuration.BarizoneProperties;
import it.barizone.integration.EmailGateway;
import org.springframework.stereotype.Component;

/** Email degli account (verifica, recupero password, esito registrazione attività). */
@Component
public class AccountMailer {

    private final EmailGateway gateway;
    private final BarizoneProperties properties;

    public AccountMailer(EmailGateway gateway, BarizoneProperties properties) {
        this.gateway = gateway;
        this.properties = properties;
    }

    public boolean enabled() {
        return gateway.enabled();
    }

    public String verification(String to, String firstName, String token) {
        String link = properties.publicOrigin() + "/verifica-email?token=" + token;
        return send(to, "Conferma il tuo indirizzo email · Barizone",
                "Ciao " + firstName + ",\n\nper attivare il tuo account Barizone conferma l’indirizzo email con questo collegamento "
                        + "(valido 48 ore):\n" + link + "\n\nSe non hai chiesto tu la registrazione, ignora questo messaggio: "
                        + "senza conferma l’account non viene attivato.\n\nLa redazione di Barizone", "verify/" + token.substring(0, 16));
    }

    public String passwordReset(String to, String firstName, String token) {
        String link = properties.publicOrigin() + "/reimposta-password?token=" + token;
        return send(to, "Reimposta la password · Barizone",
                "Ciao " + firstName + ",\n\nper scegliere una nuova password usa questo collegamento (valido 1 ora):\n" + link
                        + "\n\nSe non hai chiesto tu il recupero, ignora questo messaggio: la tua password non cambia.\n\nLa redazione di Barizone",
                "reset/" + token.substring(0, 16));
    }

    public String registrationApproved(String to, String firstName, String businessName) {
        return send(to, "La tua attività è online su Barizone",
                "Ciao " + firstName + ",\n\nabbiamo verificato i dati di " + businessName + " e la scheda è ora pubblicata su Barizone: "
                        + properties.publicOrigin() + "/aziende\n\nPuoi accedere con il tuo account per gestire i dati e scrivere ai clienti.\n\n"
                        + "La redazione di Barizone", "approved/" + to + "/" + System.nanoTime());
    }

    public String registrationRejected(String to, String firstName, String businessName, String note) {
        return send(to, "Registrazione di " + businessName + ": servono modifiche",
                "Ciao " + firstName + ",\n\nnon abbiamo potuto approvare la registrazione di " + businessName + ".\n\nMotivo: " + note
                        + "\n\nPuoi accedere al tuo account, correggere i dati e inviarli di nuovo. Per chiarimenti scrivi a "
                        + properties.contactEmail() + ".\n\nLa redazione di Barizone", "rejected/" + to + "/" + System.nanoTime());
    }

    private String send(String to, String subject, String text, String key) {
        return gateway.sendBatch(List.of(new EmailGateway.Email(properties.email().from(), List.of(to),
                properties.contactEmail(), subject, text)), key);
    }
}
