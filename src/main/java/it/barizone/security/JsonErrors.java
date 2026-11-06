package it.barizone.security;

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;

/** Risposta di errore JSON scritta direttamente dai filtri (prima dei controller). */
final class JsonErrors {

    private JsonErrors() {
    }

    static void write(HttpServletResponse response, int status, String detail) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write("{\"detail\":\"" + detail.replace("\"", "\\\"") + "\"}");
    }
}
