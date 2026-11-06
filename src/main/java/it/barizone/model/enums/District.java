package it.barizone.model.enums;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/**
 * Quartieri di Bari riconosciuti dal portale, con la stessa grafia della versione precedente.
 */
public final class District {

    public static final String ALL = "Tutta Bari";

    public static final List<String> VALUES = List.of(
            ALL, "San Nicola / Bari Vecchia", "Murat", "Madonnella", "Libertà", "Carrassi", "San Pasquale",
            "Picone", "Poggiofranco", "Japigia", "San Paolo", "San Girolamo", "Fesca", "Marconi", "Stanic",
            "Villaggio del Lavoratore", "Carbonara", "Ceglie del Campo", "Loseto", "Palese", "Santo Spirito",
            "San Pio", "Catino", "Torre a Mare", "Sant’Anna");

    private District() {
    }

    public static boolean isValid(String value) {
        return VALUES.contains(value);
    }

    /** Normalizza varianti di scrittura (accenti, maiuscole, "Bari Vecchia") nel nome canonico. */
    public static String canonical(String value) {
        String k = key(value);
        if (List.of("bari vecchia", "san nicola", "san nicola bari vecchia").contains(k)) {
            return "San Nicola / Bari Vecchia";
        }
        if (k.equals("bari")) {
            return ALL;
        }
        return VALUES.stream().filter(d -> key(d).equals(k)).findFirst()
                .orElse(value == null ? "" : value.trim());
    }

    private static String key(String value) {
        String s = value == null ? "" : value;
        s = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }
}
