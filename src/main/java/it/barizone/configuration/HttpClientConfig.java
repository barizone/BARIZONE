package it.barizone.configuration;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;

/** Timeout espliciti per ogni chiamata esterna (Resend, Open-Meteo): un servizio lento non blocca il sito. */
@Configuration
public class HttpClientConfig {

    @Bean
    RestClientCustomizer timeouts() {
        return builder -> {
            JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                    HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
            factory.setReadTimeout(Duration.ofSeconds(8));
            builder.requestFactory(factory);
        };
    }
}
