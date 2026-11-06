package it.barizone.controller.publicapi;

import java.util.LinkedHashMap;
import java.util.Map;

import it.barizone.configuration.BarizoneProperties;
import it.barizone.dto.request.AccountRequests;
import it.barizone.dto.response.AccountResponse;
import it.barizone.exception.ApiException;
import it.barizone.model.enums.AccountRole;
import it.barizone.security.CurrentAccount;
import it.barizone.security.Cookies;
import it.barizone.security.RequestThrottle;
import it.barizone.service.AccountService;
import it.barizone.service.BusinessRegistrationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Accesso di utenti e attività (l'amministratore usa /api/auth).
 * Registrazione, conferma email, login, logout e recupero password.
 */
@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final AccountService accounts;
    private final BusinessRegistrationService businesses;
    private final RequestThrottle throttle;
    private final BarizoneProperties properties;

    public AccountController(AccountService accounts, BusinessRegistrationService businesses,
                             RequestThrottle throttle, BarizoneProperties properties) {
        this.accounts = accounts;
        this.businesses = businesses;
        this.throttle = throttle;
        this.properties = properties;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody AccountRequests.Register body, HttpServletRequest request) {
        return created(throttle.limited("register", request.getRemoteAddr(), 10, () -> accounts.registerUser(body)));
    }

    @PostMapping("/register-business")
    public ResponseEntity<Map<String, Object>> registerBusiness(@RequestBody AccountRequests.RegisterBusiness body, HttpServletRequest request) {
        return created(throttle.limited("register-business", request.getRemoteAddr(), 5, () -> businesses.register(body)));
    }

    @PostMapping("/verify-email")
    public Map<String, Object> verify(@RequestBody AccountRequests.Token body, HttpServletRequest request) {
        throttle.check("verify", request.getRemoteAddr(), 30);
        AccountRole role = accounts.verifyEmail(body.token());
        return Map.of("ok", true, "role", role);
    }

    @PostMapping("/resend-verification")
    public Map<String, Object> resend(@RequestBody AccountRequests.Email body, HttpServletRequest request) {
        throttle.check("resend", request.getRemoteAddr(), 5);
        return neutral(accounts.resendVerification(body.email()));
    }

    @PostMapping("/login")
    public ResponseEntity<AccountResponse> login(@RequestBody AccountRequests.Login body, HttpServletRequest request) {
        AccountService.LoginResult result = accounts.login(body, request.getRemoteAddr());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, Cookies.lax(Cookies.ACCOUNT, result.token(), properties.accounts().sessionSeconds()))
                .body(result.account());
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Boolean>> logout(HttpServletRequest request) {
        Cookies.read(request, Cookies.ACCOUNT).ifPresent(accounts::logout);
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, Cookies.lax(Cookies.ACCOUNT, "", 0))
                .body(Map.of("success", true));
    }

    @GetMapping("/me")
    public AccountResponse me() {
        return accounts.me(CurrentAccount.get().orElseThrow(() -> ApiException.unauthorized("Accesso non effettuato.")).id());
    }

    @PostMapping("/forgot-password")
    public Map<String, Object> forgot(@RequestBody AccountRequests.Email body, HttpServletRequest request) {
        throttle.check("forgot", request.getRemoteAddr(), 5);
        return neutral(accounts.forgotPassword(body.email()));
    }

    @PostMapping("/reset-password")
    public Map<String, Boolean> reset(@RequestBody AccountRequests.ResetPassword body, HttpServletRequest request) {
        throttle.limited("reset", request.getRemoteAddr(), 20, () -> {
            accounts.resetPassword(body);
            return true;
        });
        return Map.of("ok", true);
    }

    private ResponseEntity<Map<String, Object>> created(AccountService.Outcome outcome) {
        return ResponseEntity.status(HttpStatus.CREATED).body(neutral(outcome));
    }

    /** Stessa risposta per email già registrata o nuova: non si rivela chi è iscritto. */
    private static Map<String, Object> neutral(AccountService.Outcome outcome) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", true);
        body.put("verification_required", outcome.verificationRequired());
        body.put("email_status", outcome.emailStatus());
        return body;
    }
}
