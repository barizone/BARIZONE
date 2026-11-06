package it.barizone.dto.request;

import java.util.List;

/** Scheda azienda inviata dall'admin (stessi campi della versione D1). */
public record CompanyRequest(
        String businessName, String ownerName, String legalName, String vatNumber, String taxCode,
        String legalAddress, String postalCode, String city, String province, String email, String pec,
        String phone, String category, String quartiere, String address, String description, String slogan,
        String promotion, String plan, String coverImage, String mapsUrl, String entityType,
        List<Object> photos, Boolean verified, Boolean contractActive) {
}
