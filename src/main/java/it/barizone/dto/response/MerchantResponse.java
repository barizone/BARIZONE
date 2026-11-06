package it.barizone.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import it.barizone.model.entity.CompanyProfile;
import it.barizone.model.enums.CompanyStatus;
import it.barizone.model.enums.District;
import it.barizone.model.enums.RegistrationStatus;
import java.time.Instant;

/** Scheda azienda. {@code publication_issues} è presente solo nelle risposte admin. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MerchantResponse(
        String id, String businessName, String ownerName, String legalName, String vatNumber, String taxCode,
        String legalAddress, String postalCode, String city, String province, String email, String pec,
        String phone, String category, String quartiere, String address, String description, String slogan,
        String promotion, String plan, String coverImage, String mapsUrl, String entityType, List<String> photos,
        boolean verified, boolean contractActive,
        boolean notificationsEnabled, CompanyStatus status, List<String> publicationIssues,
        RegistrationStatus registrationStatus, String reviewNote, Instant registrationAt) {

    /** Vista pubblica: senza stato di registrazione né note interne. */
    public static MerchantResponse from(CompanyProfile c) {
        return build(c, null, null, null, null);
    }

    /** Vista admin o del titolare: include stato e motivo della verifica. */
    public static MerchantResponse of(CompanyProfile c, List<String> issues) {
        return build(c, issues, c.getRegistrationStatus(), c.getReviewNote(), c.getRegistrationAt());
    }

    private static MerchantResponse build(CompanyProfile c, List<String> issues, RegistrationStatus registrationStatus,
                                          String reviewNote, Instant registrationAt) {
        return new MerchantResponse(c.getId(), c.getBusinessName(), c.getOwnerName(), c.getLegalName(),
                c.getVatNumber(), c.getTaxCode(), c.getLegalAddress(), c.getPostalCode(), c.getCity(),
                c.getProvince(), c.getEmail(), c.getPec(), c.getPhone(), c.getCategory(),
                District.canonical(c.getQuartiere()), c.getAddress(), c.getDescription(), c.getSlogan(),
                c.getPromotion(), c.getPlan(), c.getCoverImage(), c.getMapsUrl(), c.getEntityType(),
                c.getPhotos(), c.isVerified(), c.isContractActive(), c.isNotificationsEnabled(),
                c.getStatus(), issues, registrationStatus, reviewNote, registrationAt);
    }
}
