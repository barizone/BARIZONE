package it.barizone.dto.response;

import java.time.LocalDate;

/** Azienda in vetrina oggi. {@code merchant} è null se non ci sono aziende pubblicate. */
public record SpotlightResponse(LocalDate date, int intervalDays, int total, MerchantResponse merchant) {
}
