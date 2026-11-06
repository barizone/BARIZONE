package it.barizone.model.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import it.barizone.model.enums.CompanyStatus;
import it.barizone.model.enums.RegistrationStatus;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Scheda azienda. Nella versione D1 era un JSON in testo; qui ogni campo è una colonna. */
@Entity
@Table(name = "company_profiles")
@Getter
@Setter
@NoArgsConstructor
public class CompanyProfile {

    @Id
    private String id;
    private String businessName;
    private String ownerName = "";
    private String legalName = "";
    private String vatNumber = "";
    private String taxCode = "";
    private String legalAddress = "";
    private String postalCode = "";
    private String city = "";
    private String province = "";
    private String email = "";
    private String pec = "";
    private String phone = "";
    private String category = "";
    private String quartiere = "";
    private String address = "";
    private String description = "";
    private String slogan = "";
    private String promotion = "";
    private String plan = "";
    private String coverImage = "";
    private String mapsUrl = "";
    private String entityType = "";

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]")
    private List<String> photos = new ArrayList<>();

    private boolean verified;
    private boolean contractActive;
    private boolean notificationsEnabled;

    @Enumerated(EnumType.STRING)
    private CompanyStatus status = CompanyStatus.draft;

    private Instant updatedAt;

    /** Account che ha registrato l'attività (null per le schede inserite dall'admin o dal database). */
    private UUID ownerAccountId;

    @Enumerated(EnumType.STRING)
    private RegistrationStatus registrationStatus = RegistrationStatus.approved;

    private Instant registrationAt;
    private Instant reviewedAt;
    private String reviewNote = "";
    private String termsVersion;
    private Instant termsAcceptedAt;

    public boolean isPublished() {
        return status == CompanyStatus.published;
    }
}
