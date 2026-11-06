package it.barizone.integration;

/** Invio di una notifica. Restituisce lo status HTTP del servizio push (201 = accettata). */
public interface WebPushGateway {

    int send(PushTarget target, PushMessage message);

    static boolean accepted(int status) {
        return status >= 200 && status < 300;
    }

    /** 404/410: l'iscrizione non esiste più e va rimossa. */
    static boolean gone(int status) {
        return status == 404 || status == 410;
    }
}
