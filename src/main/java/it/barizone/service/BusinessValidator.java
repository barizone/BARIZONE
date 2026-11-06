package it.barizone.service;

import java.net.URI;
import java.util.Set;

import it.barizone.dto.request.AccountRequests.Business;
import it.barizone.exception.ApiException;
import it.barizone.model.enums.District;
import it.barizone.util.ItalianTaxIds;
import it.barizone.util.Texts;
import org.springframework.stereotype.Component;

/**
 * Dati dell'attività in registrazione. Obbligatori: ragione sociale, partita IVA (con cifra di controllo),
 * codice fiscale, sede legale completa, PEC, email, telefono, categoria, quartiere, indirizzo e descrizione.
 * Sono controlli formali: l'esistenza e la titolarità dell'attività le verifica l'admin prima di pubblicare.
 */
@Component
public class BusinessValidator {

    private static final Set<String> MAPS_HOSTS = Set.of("www.google.com", "google.com", "maps.google.com", "maps.app.goo.gl", "goo.gl");

    /** Dati ripuliti e validati, pronti per essere copiati nella scheda. */
    public record Clean(String businessName, String legalName, String ownerName, String vatNumber, String taxCode,
                        String legalAddress, String postalCode, String city, String province, String pec,
                        String email, String phone, String category, String quartiere, String address,
                        String description, String slogan, String promotion, String mapsUrl, String coverImage,
                        String entityType) {
    }

    public Clean validate(Business b) {
        if (b == null) {
            throw ApiException.badRequest("Inserisci i dati dell’attività.");
        }
        String businessName = required(b.businessName(), 160, "il nome dell’attività");
        String legalName = required(b.legalName(), 200, "la ragione sociale");
        String ownerName = required(b.ownerName(), 120, "nome e cognome del titolare o legale rappresentante");

        String vat = Texts.clean(b.vatNumber()).replaceAll("\\s", "");
        if (!vat.matches("^\\d{11}$")) {
            throw ApiException.badRequest("La partita IVA deve contenere 11 cifre.");
        }
        if (!ItalianTaxIds.isValidVat(vat)) {
            throw ApiException.badRequest("La partita IVA non è valida: controlla le cifre.");
        }
        String taxCode = Texts.clean(b.taxCode()).replaceAll("\\s", "").toUpperCase();
        if (taxCode.isEmpty()) {
            throw ApiException.badRequest("Inserisci il codice fiscale.");
        }
        if (!ItalianTaxIds.isValidTaxCode(taxCode)) {
            throw ApiException.badRequest("Il codice fiscale non è valido: sono 11 cifre (società) oppure 16 caratteri (persona fisica).");
        }
        String legalAddress = required(b.legalAddress(), 200, "l’indirizzo della sede legale");
        String postal = Texts.clean(b.postalCode());
        if (!postal.matches("^\\d{5}$")) {
            throw ApiException.badRequest("Il CAP deve contenere 5 cifre.");
        }
        String city = required(b.city(), 100, "il comune");
        String province = Texts.clean(b.province()).toUpperCase();
        if (!province.matches("^[A-Z]{2}$")) {
            throw ApiException.badRequest("La provincia si indica con la sigla di 2 lettere (es. BA).");
        }
        String pec = Texts.clean(b.pec()).toLowerCase();
        if (pec.length() > 254 || !Texts.EMAIL.matcher(pec).matches()) {
            throw ApiException.badRequest("Inserisci un indirizzo PEC valido.");
        }
        String email = Texts.clean(b.email()).toLowerCase();
        if (email.length() > 254 || !Texts.EMAIL.matcher(email).matches()) {
            throw ApiException.badRequest("Inserisci un’email di contatto valida.");
        }
        String phone = Texts.clean(b.phone());
        if (!phone.matches("^[+\\d\\s().-]{6,25}$")) {
            throw ApiException.badRequest("Inserisci un numero di telefono valido.");
        }
        String category = required(b.category(), 100, "la categoria");
        String quartiere = District.canonical(Texts.clean(b.quartiere()));
        if (!District.isValid(quartiere) || District.ALL.equals(quartiere)) {
            throw ApiException.badRequest("Scegli il quartiere in cui si trova l’attività.");
        }
        String address = required(b.address(), 200, "l’indirizzo dell’attività");
        String description = Texts.clean(b.description());
        if (description.length() < 20 || description.length() > 3000) {
            throw ApiException.badRequest("Descrivi l’attività in almeno 20 caratteri (massimo 3000).");
        }
        String slogan = optional(b.slogan(), 200, "Lo slogan");
        String promotion = optional(b.promotion(), 500, "La promozione");
        String entityType = optional(b.entityType(), 100, "La forma giuridica");
        String maps = Texts.clean(b.mapsUrl());
        if (!maps.isEmpty()) {
            URI uri = Texts.parseHttpUrl(maps);
            if (uri == null || !"https".equalsIgnoreCase(uri.getScheme()) || !MAPS_HOSTS.contains(uri.getHost())) {
                throw ApiException.badRequest("Inserisci un collegamento Google Maps valido.");
            }
        }
        String cover = Texts.clean(b.coverImage());
        if (!cover.isEmpty() && !Texts.isHttpsUrl(cover)) {
            throw ApiException.badRequest("Usa un collegamento HTTPS per la foto.");
        }
        return new Clean(businessName, legalName, ownerName, vat, taxCode, legalAddress, postal, city, province, pec,
                email, phone, category, quartiere, address, description, slogan, promotion, maps, cover, entityType);
    }

    private static String required(String value, int max, String what) {
        String v = Texts.clean(value);
        if (v.isEmpty() || v.length() > max) {
            throw ApiException.badRequest("Inserisci " + what + " (massimo " + max + " caratteri).");
        }
        return v;
    }

    private static String optional(String value, int max, String what) {
        String v = Texts.clean(value);
        if (v.length() > max) {
            throw ApiException.badRequest(what + " può avere al massimo " + max + " caratteri.");
        }
        return v;
    }
}
