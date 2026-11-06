package it.barizone.security;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

/** Le API riservate rispondono 403 JSON (come la versione Worker), mai con una pagina di login. */
public class RestAccessHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

    private static final String DETAIL = "Accesso riservato all’amministratore.";

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e)
            throws IOException {
        JsonErrors.write(response, HttpServletResponse.SC_FORBIDDEN, DETAIL);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException e)
            throws IOException {
        JsonErrors.write(response, HttpServletResponse.SC_FORBIDDEN, DETAIL);
    }
}
