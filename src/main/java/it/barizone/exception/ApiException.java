package it.barizone.exception;

import java.util.Map;

import org.springframework.http.HttpStatus;

/**
 * Errore applicativo con messaggio in italiano destinato all'utente.
 * Viene serializzato come {@code {"detail": "..."}}, il formato atteso dal frontend.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final transient Map<String, Object> extra;

    public ApiException(HttpStatus status, String detail) {
        this(status, detail, Map.of());
    }

    public ApiException(HttpStatus status, String detail, Map<String, Object> extra) {
        super(detail);
        this.status = status;
        this.extra = extra;
    }

    public HttpStatus status() {
        return status;
    }

    public Map<String, Object> extra() {
        return extra;
    }

    public static ApiException badRequest(String detail) {
        return new ApiException(HttpStatus.BAD_REQUEST, detail);
    }

    public static ApiException notFound(String detail) {
        return new ApiException(HttpStatus.NOT_FOUND, detail);
    }

    public static ApiException forbidden(String detail) {
        return new ApiException(HttpStatus.FORBIDDEN, detail);
    }

    public static ApiException unauthorized(String detail) {
        return new ApiException(HttpStatus.UNAUTHORIZED, detail);
    }

    public static ApiException conflict(String detail) {
        return new ApiException(HttpStatus.CONFLICT, detail);
    }
}
