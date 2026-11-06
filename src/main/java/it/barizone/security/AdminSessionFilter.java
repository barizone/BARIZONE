package it.barizone.security;

import java.io.IOException;
import java.util.List;

import it.barizone.service.AdminAuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Legge il cookie di sessione admin e popola il SecurityContext con il ruolo ADMIN. */
public class AdminSessionFilter extends OncePerRequestFilter {

    private final AdminAuthService authService;

    public AdminSessionFilter(AdminAuthService authService) {
        this.authService = authService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Cookies.read(request, Cookies.ADMIN).flatMap(authService::resolve).ifPresent(admin ->
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                        admin, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))));
        chain.doFilter(request, response);
    }
}
