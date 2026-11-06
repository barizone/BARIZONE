package it.barizone.controller.publicapi;

import java.util.Map;

import it.barizone.configuration.BarizoneProperties;
import it.barizone.dto.request.LoginRequest;
import it.barizone.dto.response.UserResponse;
import it.barizone.exception.ApiException;
import it.barizone.security.Cookies;
import it.barizone.security.CurrentAdmin;
import it.barizone.service.AdminAuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AdminAuthService authService;
    private final BarizoneProperties properties;

    public AuthController(AdminAuthService authService, BarizoneProperties properties) {
        this.authService = authService;
        this.properties = properties;
    }

    @GetMapping("/me")
    public UserResponse me() {
        return CurrentAdmin.get()
                .map(a -> new UserResponse(a.id(), a.name(), a.email(), "admin"))
                .orElseThrow(() -> ApiException.unauthorized("Accesso non effettuato."));
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@RequestBody LoginRequest body, HttpServletRequest request) {
        AdminAuthService.LoginResult result = authService.login(body.username(), body.password(), request.getRemoteAddr());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, Cookies.lax(Cookies.ADMIN, result.token(), properties.admin().sessionSeconds()))
                .body(result.user());
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Boolean>> logout(HttpServletRequest request) {
        Cookies.read(request, Cookies.ADMIN).ifPresent(authService::logout);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, Cookies.lax(Cookies.ADMIN, "", 0))
                .body(Map.of("success", true));
    }
}
