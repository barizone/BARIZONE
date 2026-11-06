package it.barizone.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.barizone.model.entity.CompanyProfile;
import it.barizone.model.enums.CompanyStatus;
import it.barizone.model.enums.District;

/** Scheda azienda. {@code publication_issues} è presente solo nelle risposte admin. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MerchantResponse(
        String id, String businessName, String ownerName, String legalName, String vatNumber, String taxCode,
        String legalAddress, String postalCode, String city, String province, String email, String pec,
        String phone, String category, String quartiere, String address, String description, String slogan,
        String promotion, String plan, String coverImage, String mapsUrl, String entityType, List<String> photos,
        @JsonProperty("is_demo") boolean demo, boolean verified, boolean contractActive,
        boolean notificationsEnabled, CompanyStatus status, List<String> publicationIssues) {

    public static MerchantResponse from(CompanyProfile c) {
        return of(c, null);
    }

    public static MerchantResponse of(CompanyProfile c, List<String> issues) {
        return new MerchantResponse(c.getId(), c.getBusinessName(), c.getOwnerName(), c.getLegalName(),
                c.getVatNumber(), c.getTaxCode(), c.getLegalAddress(), c.getPostalCode(), c.getCity(),
                c.getProvince(), c.getEmail(), c.getPec(), c.getPhone(), c.getCategory(),
                District.canonical(c.getQuartiere()), c.getAddress(), c.getDescription(), c.getSlogan(),
                c.getPromotion(), c.getPlan(), c.getCoverImage(), c.getMapsUrl(), c.getEntityType(),
                c.getPhotos(), c.isDemo(), c.isVerified(), c.isContractActive(), c.isNotificationsEnabled(),
                c.getStatus(), issues);
    }
}
