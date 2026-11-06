package it.barizone.unit;

import static org.assertj.core.api.Assertions.assertThat;

import it.barizone.util.ItalianTaxIds;
import org.junit.jupiter.api.Test;

class ItalianTaxIdsTest {

    @Test
    void vatChecksum() {
        assertThat(ItalianTaxIds.isValidVat("01234567897")).isTrue();
        assertThat(ItalianTaxIds.isValidVat("12345678903")).isTrue();
        assertThat(ItalianTaxIds.isValidVat("01234567890")).as("cifra di controllo errata").isFalse();
        assertThat(ItalianTaxIds.isValidVat("00000000000")).isFalse();
        assertThat(ItalianTaxIds.isValidVat("1234567890")).as("10 cifre").isFalse();
        assertThat(ItalianTaxIds.isValidVat("1234567890A")).isFalse();
        assertThat(ItalianTaxIds.isValidVat(null)).isFalse();
    }

    @Test
    void taxCodeForPersonAndCompany() {
        // Codice fiscale di esempio usato nella documentazione ufficiale e nei test di molte librerie
        assertThat(ItalianTaxIds.isValidTaxCode("RSSMRA85T10A562S")).isTrue();
        assertThat(ItalianTaxIds.isValidTaxCode("rssmra85t10a562s")).as("minuscole accettate").isTrue();
        assertThat(ItalianTaxIds.isValidTaxCode("RSSMRA85T10A562X")).as("carattere di controllo errato").isFalse();
        assertThat(ItalianTaxIds.isValidTaxCode("RSSMRA85T10A562")).as("15 caratteri").isFalse();
        assertThat(ItalianTaxIds.isValidTaxCode("01234567897")).as("società: 11 cifre valide").isTrue();
        assertThat(ItalianTaxIds.isValidTaxCode("01234567890")).isFalse();
        assertThat(ItalianTaxIds.isValidTaxCode("")).isFalse();
        assertThat(ItalianTaxIds.isValidTaxCode(null)).isFalse();
    }
}
