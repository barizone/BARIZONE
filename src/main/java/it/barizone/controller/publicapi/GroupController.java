package it.barizone.controller.publicapi;

import java.util.Map;
import java.util.Optional;

import it.barizone.exception.ApiException;
import it.barizone.integration.VapidKeyProvider;
import it.barizone.model.entity.MerchantClient;
import it.barizone.security.Cookies;
import it.barizone.security.CurrentAdmin;
import it.barizone.service.GroupService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** Gruppi esercenti (/gestione-clienti, /esercente, /adesioni). */
@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private static final int MAX_BODY = 10_000;
    private final GroupService groups;
    private final VapidKeyProvider keys;

    public GroupController(GroupService groups, VapidKeyProvider keys) {
        this.groups = groups;
        this.keys = keys;
    }

    @GetMapping("/me")
    public Map<String, Object> me(HttpServletRequest request) {
        return java.util.Collections.singletonMap("user", staff(request).map(GroupService::staffJson).orElse(null));
    }

    @PostMapping("/merchant-login")
    public ResponseEntity<Map<String, Object>> merchantLogin(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        limit(request);
        Map.Entry<String, String> session = groups.merchantLogin(body.get("token"));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, Cookies.strict(Cookies.MERCHANT, session.getValue(), GroupService.STAFF_LIFETIME_SECONDS))
                .body(Map.of("role", "merchant", "shop_id", session.getKey()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Boolean>> logout(HttpServletRequest request) {
        groups.merchantLogout(Cookies.read(request, Cookies.MERCHANT));
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, Cookies.strict(Cookies.MERCHANT, "", 0))
                .body(Map.of("ok", true));
    }

    @GetMapping("/state")
    public Map<String, Object> state(@RequestParam(required = false) String activity, HttpServletRequest request) {
        GroupService.Staff user = staff(request).orElseThrow(() -> ApiException.unauthorized("Accesso riservato."));
        return groups.state(user, "1".equals(activity));
    }

    @GetMapping("/shops/{id}")
    public Map<String, Object> shop(@PathVariable String id) {
        return groups.shop(id);
    }

    @PostMapping("/shops/{id}/access")
    public Map<String, Object> access(@PathVariable String id, HttpServletRequest request) {
        return groups.grantAccess(staff(request).orElse(null), id,
                ServletUriComponentsBuilder.fromContextPath(request).replacePath(null).build().toUriString());
    }

    @PostMapping("/shops/{id}/messages")
    public ResponseEntity<Map<String, Object>> message(@PathVariable String id, @RequestBody(required = false) Map<String, Object> body,
                                                       HttpServletRequest request) {
        limit(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(groups.sendMessage(staff(request).orElse(null), id, null, body == null ? Map.of() : body));
    }

    @PostMapping("/shops/{id}/messages/{messageId}/send")
    public ResponseEntity<Map<String, Object>> resend(@PathVariable String id, @PathVariable String messageId,
                                                      HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(groups.sendMessage(staff(request).orElse(null), id, messageId, Map.of()));
    }

    @PostMapping("/subscribe")
    public ResponseEntity<Map<String, Boolean>> subscribe(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        limit(request);
        Optional<String> token = groups.subscribe(client(request), body);
        ResponseEntity.BodyBuilder response = ResponseEntity.status(HttpStatus.CREATED);
        token.ifPresent(t -> response.header(HttpHeaders.SET_COOKIE,
                Cookies.strict(Cookies.CUSTOMER, t, GroupService.CLIENT_LIFETIME_SECONDS)));
        return response.body(Map.of("ok", true));
    }

    @GetMapping("/inbox")
    public Map<String, Object> inbox(HttpServletRequest request) {
        return groups.inbox(client(request));
    }

    @PostMapping("/unsubscribe")
    public Map<String, Boolean> unsubscribe(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        groups.unsubscribe(client(request), body);
        return Map.of("ok", true);
    }

    @GetMapping("/push/key")
    public Map<String, String> pushKey() {
        return Map.of("publicKey", keys.keys().publicKey());
    }

    @PostMapping("/push/subscription")
    public Map<String, Boolean> enablePush(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        limit(request);
        groups.enablePush(client(request), body);
        return Map.of("ok", true);
    }

    @DeleteMapping("/push/subscription")
    public Map<String, Boolean> disablePush(HttpServletRequest request) {
        groups.disablePush(client(request));
        return Map.of("ok", true);
    }

    private Optional<GroupService.Staff> staff(HttpServletRequest request) {
        return groups.staff(CurrentAdmin.get(), Cookies.read(request, Cookies.MERCHANT));
    }

    private Optional<MerchantClient> client(HttpServletRequest request) {
        return groups.client(Cookies.read(request, Cookies.CUSTOMER));
    }


    private static void limit(HttpServletRequest request) {
        if (request.getContentLengthLong() > MAX_BODY) {
            throw ApiException.badRequest("Dati non validi.");
        }
    }
}
