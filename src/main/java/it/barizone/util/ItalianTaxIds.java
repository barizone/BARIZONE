package it.barizone.util;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Controlli formali su partita IVA e codice fiscale italiani: lunghezza, formato e cifra di controllo.
 * Verificano che il numero sia scritto correttamente, non che esista o appartenga all'attività:
 * quello lo controlla l'admin prima di approvare la registrazione.
 */
public final class ItalianTaxIds {

    private static final Pattern PERSON_CF = Pattern.compile("^[A-Z]{6}[0-9LMNPQRSTUV]{2}[A-Z][0-9LMNPQRSTUV]{2}[A-Z][0-9LMNPQRSTUV]{3}[A-Z]$");
    private static final Pattern COMPANY_CF = Pattern.compile("^\\d{11}$");

    private static final Map<Character, Integer> ODD = oddTable();

    private ItalianTaxIds() {
    }

    /** Partita IVA: 11 cifre, ultima = cifra di controllo (algoritmo di Luhn adattato). */
    public static boolean isValidVat(String vat) {
        if (vat == null || !vat.matches("^\\d{11}$") || vat.startsWith("0000000")) {
            return false;
        }
        int sum = 0;
        for (int i = 0; i < 10; i++) {
            int d = vat.charAt(i) - '0';
            if (i % 2 == 1) {
                d *= 2;
                if (d > 9) {
                    d -= 9;
                }
            }
            sum += d;
        }
        return (10 - sum % 10) % 10 == vat.charAt(10) - '0';
    }

    /** Codice fiscale: 11 cifre (società) oppure 16 caratteri (persona fisica, ditta individuale). */
    public static boolean isValidTaxCode(String code) {
        if (code == null) {
            return false;
        }
        String c = code.trim().toUpperCase();
        if (COMPANY_CF.matcher(c).matches()) {
            return isValidVat(c);
        }
        if (!PERSON_CF.matcher(c).matches()) {
            return false;
        }
        int sum = 0;
        for (int i = 0; i < 15; i++) {
            char ch = c.charAt(i);
            sum += i % 2 == 0 ? ODD.get(ch) : Character.isDigit(ch) ? ch - '0' : ch - 'A';
        }
        return c.charAt(15) == (char) ('A' + sum % 26);
    }

    private static Map<Character, Integer> oddTable() {
        // Valori dei caratteri in posizione dispari (1ª, 3ª, ...): cifre 0-9 e lettere A-Z.
        int[] values = {1, 0, 5, 7, 9, 13, 15, 17, 19, 21, 2, 4, 18, 20, 11, 3, 6, 8, 12, 14, 16, 10, 22, 25, 24, 23};
        Map<Character, Integer> table = new java.util.HashMap<>();
        for (int i = 0; i < 10; i++) {
            table.put((char) ('0' + i), values[i]);
        }
        for (int i = 0; i < 26; i++) {
            table.put((char) ('A' + i), values[i]);
        }
        return table;
    }
}
