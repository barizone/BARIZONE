package it.barizone.security;

import java.io.IOException;
import java.util.List;

import it.barizone.service.AccountService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Legge il cookie di sessione dell'account e assegna il ruolo USER o MERCHANT. */
public class AccountSessionFilter extends OncePerRequestFilter {

    private final AccountService accounts;

    public AccountSessionFilter(AccountService accounts) {
        this.accounts = accounts;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Cookies.read(request, Cookies.ACCOUNT).flatMap(accounts::resolveSession).ifPresent(account ->
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                        account, null, List.of(new SimpleGrantedAuthority("ROLE_" + account.role().name().toUpperCase())))));
        chain.doFilter(request, response);
    }
}
