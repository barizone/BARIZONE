package it.barizone.configuration;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Applicazione a pagina singola: ogni percorso di pagina (senza estensione, fuori da /api)
 * riceve index.html, e il router React mostra la schermata giusta. L'HTML non va in cache,
 * così un nuovo deploy è visibile subito.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class SpaFallbackFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !"GET".equals(request.getMethod()) || path.startsWith("/api/") || path.startsWith("/actuator")
                || path.equals("/sitemap.xml") || path.equals("/robots.txt")
                || path.substring(path.lastIndexOf('/') + 1).contains(".");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        request.getRequestDispatcher("/index.html").forward(request, new jakarta.servlet.http.HttpServletResponseWrapper(response) {
            // index.html non va mai in cache: un nuovo deploy deve essere visibile subito.
            @Override
            public void setHeader(String name, String value) {
                super.setHeader(name, "Cache-Control".equalsIgnoreCase(name) ? "no-store" : value);
            }
        });
        response.setHeader("Cache-Control", "no-store");
    }
}
