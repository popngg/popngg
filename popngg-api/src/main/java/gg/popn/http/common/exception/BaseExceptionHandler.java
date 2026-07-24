package gg.popn.http.common.exception;

import gg.popn.domain.common.exception.BaseException;
import gg.popn.application.auth.exception.InvalidPasswordResetTokenException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class BaseExceptionHandler {
    @ExceptionHandler(InvalidPasswordResetTokenException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidPasswordResetToken() {
        return ResponseEntity
                .badRequest()
                .body(Map.of(
                        "code", "INVALID_PASSWORD_RESET_TOKEN",
                        "message", "The password reset token is invalid or expired."));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationException() {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(
                        "code", "UNAUTHORIZED",
                        "message", "Invalid credentials"));
    }

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<Map<String, Object>> handleBaseException(HttpServletRequest request, BaseException e) {
        if (e.getCode().getStatusCode() >= HttpStatus.INTERNAL_SERVER_ERROR.value()) {
            log.error("BaseException occurred. requestURL={}, method={}", request.getRequestURL(), request.getMethod(), e);
        }
        else {
            log.info("BaseException occurred. requestURL={}, method={}", request.getRequestURL(), request.getMethod(), e);
        }

        return ResponseEntity
                .status(e.getCode().getStatusCode())
                .body(toMap(e));
    }

    private static Map<String, Object> toMap(BaseException e) {
        Map<String, Object> map = new HashMap<>();
        map.put("code", e.getCode());
        map.put("message", e.getMessage());
        if (e.getErrorDetail() != null) {
            map.put("errorDetail", e.getErrorDetail());
        }
        return map;
    }
}
