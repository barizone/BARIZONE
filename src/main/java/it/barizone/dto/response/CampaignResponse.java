package it.barizone.dto.response;

import java.time.Instant;
import java.util.UUID;

import it.barizone.model.entity.PushCampaign;
import it.barizone.model.enums.CampaignKind;
import it.barizone.model.enums.CampaignStatus;
import it.barizone.model.enums.PaymentStatus;

public record CampaignResponse(UUID id, String title, String body, String url, String business, String contractRef,
                               long amountCents, PaymentStatus paymentStatus, String quartiere, CampaignStatus status,
                               Instant createdAt, Instant startedAt, String createdBy, long accepted, long failed,
                               CampaignKind kind) {

    public static CampaignResponse of(PushCampaign c, long accepted, long failed) {
        return new CampaignResponse(c.getId(), c.getTitle(), c.getBody(), c.getUrl(), c.getBusiness(),
                c.getContractRef(), c.getAmountCents(), c.getPaymentStatus(), c.getQuartiere(), c.getStatus(),
                c.getCreatedAt(), c.getStartedAt(), c.getCreatedBy(), accepted, failed, c.getKind());
    }
}
