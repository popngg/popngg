package gg.popn.http.common.exception;

import gg.popn.domain.common.exception.BaseException;
import gg.popn.application.user.exception.UserProfileNotFoundException;
import gg.popn.application.auth.exception.InvalidPasswordResetTokenException;
import gg.popn.application.auth.exception.AlreadyRegisteredException;
import gg.popn.http.renewal.RenewalException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import gg.popn.application.song.exception.CatalogItemNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class BaseExceptionHandler {
    @ExceptionHandler(RenewalException.class)
    public ResponseEntity<Map<String,Object>> handleRenewal(RenewalException exception){return ResponseEntity.status(exception.status()).body(Map.of("code",exception.code(),"message",exception.getMessage()));}
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String,Object>> handleInvalidPayload(MethodArgumentNotValidException exception){
        boolean emptyCharts = exception.getBindingResult().getFieldErrors().stream()
                .anyMatch(error -> "charts".equals(error.getField()) && "NotEmpty".equals(error.getCode()));
        boolean payloadTooLarge = exception.getBindingResult().getFieldErrors().stream()
                .anyMatch(error -> "stats.payloadBytes".equals(error.getField()) && "Max".equals(error.getCode()));
        if (payloadTooLarge) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(Map.of(
                    "code", "PAYLOAD_TOO_LARGE", "message", "The renewal payload exceeds 4 MiB."));
        }
        String code = emptyCharts ? "EMPTY_PAYLOAD" : "INVALID_PAYLOAD";
        String message = emptyCharts ? "At least one played chart is required." : "The request payload is invalid.";
        return ResponseEntity.badRequest().body(Map.of("code",code,"message",message));
    }
    @ExceptionHandler(AlreadyRegisteredException.class)
    public ResponseEntity<Map<String, Object>> handleAlreadyRegistered() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("code", "ALREADY_REGISTERED", "message", "The poptomo ID is already registered."));
    }
    @ExceptionHandler(CatalogItemNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleCatalogItemNotFound(CatalogItemNotFoundException exception) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("code", "CATALOG_ITEM_NOT_FOUND", "message", exception.getMessage()));
    }

    @ExceptionHandler(UserProfileNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserProfileNotFound() {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("code", "USER_PROFILE_NOT_FOUND", "message", "User profile not found."));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied() {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(Map.of("code", "FORBIDDEN", "message", "Profile update is not allowed."));
    }

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
