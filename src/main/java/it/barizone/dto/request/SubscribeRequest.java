package it.barizone.dto.request;

/** Campi Object: la validazione distingue "stringa" da altri tipi, come la versione precedente. */
public record SubscribeRequest(Object firstName, Object lastName, Object email, Object phone, Object quartiere,
                               Object consent, Object consentVersion, Object website) {
}
