package it.barizone.unit;

import static org.assertj.core.api.Assertions.assertThat;

import it.barizone.security.PasswordHasher;
import it.barizone.util.Tokens;
import org.junit.jupiter.api.Test;

/** Valori di riferimento calcolati con il codice originale (WebCrypto, server/auth.js). */
class PasswordHasherTest {

    @Test
    void pbkdf2MatchesWebCryptoImplementation() {
        String hash = new PasswordHasher().hash("Password-Di-Prova!", "a71473db422fdf6d497dc31e1cfbef9e73202215495396d5", 100_000);
        assertThat(hash).isEqualTo("67d94092a0375ee4a2790f02bc2dd6013d5ac05b1a20d043211c7f6e901976af");
    }

    @Test
    void sha256HexMatchesTokenHash() {
        assertThat(Tokens.sha256Hex("abc")).isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void randomTokensAre64HexChars() {
        assertThat(Tokens.randomToken()).matches("^[a-f0-9]{64}$").isNotEqualTo(Tokens.randomToken());
    }
}
