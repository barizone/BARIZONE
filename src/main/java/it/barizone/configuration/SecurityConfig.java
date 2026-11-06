package it.barizone.configuration;

import it.barizone.security.AccountSessionFilter;
import it.barizone.security.AdminPageFilter;
import it.barizone.security.AdminSessionFilter;
import it.barizone.security.OriginCheckFilter;
import it.barizone.security.RestAccessHandlers;
import it.barizone.service.AccountService;
import it.barizone.service.AdminAuthService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

/**
 * Sicurezza HTTP.
 * <ul>
 *   <li>Nessuna sessione server: l'identità arriva dal cookie {@code __Host-} a ogni richiesta.</li>
 *   <li>CSRF: controllo dell'header Origin ({@link OriginCheckFilter}) invece del token sincronizzato,
 *       come nella versione Worker; i cookie sono anche SameSite.</li>
 *   <li>Tre accessi: ADMIN ({@code /api/admin/**}), MERCHANT = attività ({@code /api/account/business/**}),
 *       USER = utente registrato; tutto il resto è pubblico.</li>
 * </ul>
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AdminAuthService authService,
                                            AccountService accountService, BarizoneProperties properties) throws Exception {
        RestAccessHandlers handlers = new RestAccessHandlers();
        http
                .csrf(csrf -> csrf.disable())
                .httpBasic(b -> b.disable())
                .formLogin(f -> f.disable())
                .logout(l -> l.disable())
                .requestCache(c -> c.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new OriginCheckFilter(properties), AnonymousAuthenticationFilter.class)
                .addFilterBefore(new AccountSessionFilter(accountService), AnonymousAuthenticationFilter.class)
                .addFilterBefore(new AdminSessionFilter(authService), AnonymousAuthenticationFilter.class)
                .addFilterAfter(new AdminPageFilter(), AnonymousAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/account/business/**").hasRole("MERCHANT")
                        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        .anyRequest().permitAll())
                .exceptionHandling(e -> e.authenticationEntryPoint(handlers).accessDeniedHandler(handlers))
                .headers(h -> h
                        // La cache è decisa per tipo di risorsa in WebConfig (API no-store, asset immutabili 1 anno).
                        .cacheControl(c -> c.disable())
                        .frameOptions(f -> f.deny())
                        .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .permissionsPolicyHeader(p -> p.policy("camera=(), microphone=(), geolocation=()")));
        return http.build();
    }
}
