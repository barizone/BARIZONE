package it.barizone.dto.response;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import it.barizone.model.enums.AccountRole;
import it.barizone.model.enums.AccountStatus;

/** Account autenticato. Per le attività include lo stato della registrazione. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AccountResponse(UUID id, String email, String firstName, String lastName, String name,
                              AccountRole role, AccountStatus status, BusinessSummary business) {

    public record BusinessSummary(String id, String businessName, String registrationStatus, String status,
                                  String reviewNote, boolean published) {
    }
}
