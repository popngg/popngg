package gg.popn.http.user;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class CsrfController {
    /** Keep the issued cookie and send this masked token in headerName on subsequent unsafe requests. */
    @GetMapping("/api/v1/auth/csrf")
    public ResponseEntity<Map<String, String>> token(CsrfToken token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(Map.of("headerName", token.getHeaderName(), "token", token.getToken()));
    }
}
