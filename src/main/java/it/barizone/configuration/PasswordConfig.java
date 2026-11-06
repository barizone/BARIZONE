package it.barizone.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Password degli account utente e attività: BCrypt con costo configurabile (12 in produzione). */
@Configuration
public class PasswordConfig {

    @Bean
    PasswordEncoder passwordEncoder(BarizoneProperties properties) {
        return new BCryptPasswordEncoder(properties.accounts().bcryptStrength());
    }
}
