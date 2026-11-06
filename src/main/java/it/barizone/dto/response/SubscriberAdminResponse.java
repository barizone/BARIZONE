package it.barizone.dto.response;

import java.time.Instant;

public record SubscriberAdminResponse(String id, String firstName, String lastName, String email, String phone,
                                      String quartiere, Instant consentAt, String status, String consentVersion,
                                      int pushRequested, long pushDevices) {
}
