package it.barizone.dto.response;

public record ContactResponse(String email, boolean emailEnabled, String whatsappNumber, String whatsappUrl,
                              boolean published) {
}
