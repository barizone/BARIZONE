package it.barizone.controller.publicapi;

import java.util.Map;

import it.barizone.integration.WeatherClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Nuovo: meteo servito dal backend con cache (prima ogni browser chiamava Open-Meteo direttamente). */
@RestController
public class WeatherController {

    private final WeatherClient weatherClient;

    public WeatherController(WeatherClient weatherClient) {
        this.weatherClient = weatherClient;
    }

    @GetMapping("/api/weather")
    public Map<String, Object> weather() {
        return weatherClient.forecast();
    }
}
