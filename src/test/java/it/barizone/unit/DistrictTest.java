package it.barizone.unit;

import static org.assertj.core.api.Assertions.assertThat;

import it.barizone.model.enums.District;
import org.junit.jupiter.api.Test;

class DistrictTest {

    @Test
    void canonicalNamesIgnoreAccentsCaseAndAliases() {
        assertThat(District.canonical("bari vecchia")).isEqualTo("San Nicola / Bari Vecchia");
        assertThat(District.canonical("LIBERTA")).isEqualTo("Libertà");
        assertThat(District.canonical("Bari")).isEqualTo("Tutta Bari");
        assertThat(District.canonical("  Sconosciuto ")).isEqualTo("Sconosciuto");
        assertThat(District.canonical(null)).isEmpty();
    }
}
