package it.barizone.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Traduce ogni errore nel formato {@code {"detail": "..."}} usato dalla versione precedente.
 * Gli errori imprevisti non espongono dettagli interni: il messaggio rassicura l'utente
 * che i dati inseriti restano nel modulo.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public static ResponseEntity<Map<String, Object>> body(HttpStatus status, String detail) {
        return body(status, detail, Map.of());
    }

    public static ResponseEntity<Map<String, Object>> body(HttpStatus status, String detail, Map<String, Object> extra) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("detail", detail);
        payload.putAll(extra);
        return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(payload);
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> api(ApiException e) {
        return body(e.status(), e.getMessage(), e.extra());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, Object>> unreadable(Exception e) {
        return body(HttpStatus.BAD_REQUEST, "Dati non validi.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> invalid(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getAllErrors().stream()
                .map(err -> err.getDefaultMessage())
                .findFirst().orElse("Dati non validi.");
        return body(HttpStatus.BAD_REQUEST, detail);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> tooLarge(MaxUploadSizeExceededException e) {
        return body(HttpStatus.PAYLOAD_TOO_LARGE, "Foto troppo grande: massimo 1 MB.");
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<Map<String, Object>> multipart(MultipartException e) {
        return body(HttpStatus.BAD_REQUEST, "Carica una foto JPG, PNG o WebP fino a 1 MB.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> method(HttpRequestMethodNotSupportedException e) {
        return body(HttpStatus.METHOD_NOT_ALLOWED, "Operazione non disponibile.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> notFound(NoResourceFoundException e) {
        return body(HttpStatus.NOT_FOUND, "Funzione non disponibile.");
    }

    /** Vincoli di unicità violati da richieste simultanee (stessa email, stessa partita IVA). */
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> integrity(org.springframework.dao.DataIntegrityViolationException e) {
        log.warn("Vincolo del database violato: {}", e.getMostSpecificCause().getMessage());
        return body(HttpStatus.CONFLICT, "Esiste già una registrazione con questi dati. Riprova accedendo con il tuo account.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> unexpected(Exception e) {
        log.error("barizone-api", e);
        return body(HttpStatus.SERVICE_UNAVAILABLE,
                "Servizio temporaneamente non disponibile. Riprova: i dati inseriti rimangono nel modulo.");
    }
}
