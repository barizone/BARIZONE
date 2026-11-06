package it.barizone.dto.response;

/** Esito di un lotto di invio. {@code more} = ci sono ancora destinatari da raggiungere. */
public record SendResult(int accepted, int failed, boolean more) {
}
