package it.barizone.dto.request;

/** Richieste degli account. I campi sono tipizzati; la validazione dei contenuti è nei servizi. */
public final class AccountRequests {

    private AccountRequests() {
    }

    public record Register(String firstName, String lastName, String email, String password, Boolean acceptTerms) {
    }

    public record Login(String email, String password) {
    }

    public record Token(String token) {
    }

    public record Email(String email) {
    }

    public record ResetPassword(String token, String password) {
    }

    /** Registrazione di un'attività: account del titolare + dati dell'attività + dichiarazioni. */
    public record RegisterBusiness(String firstName, String lastName, String email, String password,
                                   Boolean acceptTerms, Boolean representsBusiness, Business business) {
    }

    /** Dati dell'attività. Tutti obbligatori tranne slogan, promozione, mappa, foto e forma giuridica. */
    public record Business(String businessName, String legalName, String ownerName, String vatNumber, String taxCode,
                           String legalAddress, String postalCode, String city, String province, String pec,
                           String email, String phone, String category, String quartiere, String address,
                           String description, String slogan, String promotion, String mapsUrl, String coverImage,
                           String entityType) {
    }
}
