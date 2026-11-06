package it.barizone.model.entity;

import java.time.Instant;
import java.util.UUID;

import it.barizone.model.enums.CampaignKind;
import it.barizone.model.enums.CampaignStatus;
import it.barizone.model.enums.PaymentStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "push_campaigns")
@Getter
@Setter
@NoArgsConstructor
public class PushCampaign {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    private CampaignKind kind = CampaignKind.sponsored;

    private String title;
    private String body;
    private String url;
    private String business = "";
    private String contractRef = "";
    private long amountCents;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus = PaymentStatus.pending;

    private String quartiere;

    @Enumerated(EnumType.STRING)
    private CampaignStatus status = CampaignStatus.draft;

    private Instant createdAt;
    private Instant startedAt;
    private String createdBy;

    /** Una campagna commerciale parte solo con committente, accordo, importo e pagamento confermato. */
    public boolean readyToSend() {
        return kind == CampaignKind.barizone
                || (paymentStatus == PaymentStatus.paid && !business.isBlank()
                    && !contractRef.isBlank() && amountCents > 0);
    }
}
