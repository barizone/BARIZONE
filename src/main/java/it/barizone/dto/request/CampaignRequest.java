package it.barizone.dto.request;

public record CampaignRequest(Object title, Object body, Object url, Object business, Object contractRef,
                              Object amountCents, Object paymentStatus, Object quartiere, Object kind) {
}
