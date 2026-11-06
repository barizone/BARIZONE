package it.barizone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BarizoneApplication {

    public static void main(String[] args) {
        SpringApplication.run(BarizoneApplication.class, args);
    }
}
