package it.barizone.security;

import java.io.IOException;
import java.util.Set;

import it.barizone.configuration.BarizoneProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Protezione CSRF: ogni richiesta API che modifica dati deve arrivare dallo stesso origin del sito
 * (o da un origin configurato). Stesso comportamento della versione Worker.
 */
public class OriginCheckFilter extends OncePerRequestFilter {

    private static final Set<String> SAFE = Set.of("GET", "HEAD", "OPTIONS");
    private final BarizoneProperties properties;

    public OriginCheckFilter(BarizoneProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return SAFE.contains(request.getMethod()) || !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String origin = request.getHeader("Origin");
        if (origin == null || !(origin.equals(requestOrigin(request)) || properties.trustedOrigins().contains(origin))) {
            JsonErrors.write(response, HttpServletResponse.SC_FORBIDDEN, "Richiesta non autorizzata.");
            return;
        }
        chain.doFilter(request, response);
    }

    static String requestOrigin(HttpServletRequest request) {
        String scheme = request.getScheme();
        int port = request.getServerPort();
        boolean defaultPort = ("https".equals(scheme) && port == 443) || ("http".equals(scheme) && port == 80);
        return scheme + "://" + request.getServerName() + (defaultPort ? "" : ":" + port);
    }
}
