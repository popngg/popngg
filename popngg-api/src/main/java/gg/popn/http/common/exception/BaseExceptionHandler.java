package gg.popn.http.common.exception;

import gg.popn.domain.common.exception.BaseException;
import gg.popn.application.user.exception.UserProfileNotFoundException;
import gg.popn.application.auth.exception.InvalidPasswordResetTokenException;
import gg.popn.application.auth.exception.AlreadyRegisteredException;
import gg.popn.http.renewal.RenewalException;
import gg.popn.application.playdata.service.PlaydataUpsertPolicy.MissingGameVersionTransitionException;
import gg.popn.application.playdata.exception.DuplicatePlaydataRowIdentityException;
import gg.popn.application.playdata.exception.ActualPopclassUnavailableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
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
import gg.popn.application.common.ErrorNotificationPort;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@RestControllerAdvice
public class BaseExceptionHandler {
    private final ErrorNotificationPort errorNotification;

    public BaseExceptionHandler() {
        this((method, path, exceptionType, traceId) -> {});
    }

    @Autowired
    public BaseExceptionHandler(ErrorNotificationPort errorNotification) {
        this.errorNotification = errorNotification;
    }
    @ExceptionHandler(ActualPopclassUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleActualPopclassUnavailable(
            ActualPopclassUnavailableException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "code", "ACTUAL_POPN_CLASS_UNAVAILABLE",
                "message", exception.getMessage()));
    }

    @ExceptionHandler(DuplicatePlaydataRowIdentityException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicatePlaydataRowIdentity(
            DuplicatePlaydataRowIdentityException exception) {
        return ResponseEntity.unprocessableEntity().body(Map.of(
                "code", "DUPLICATE_CHART",
                "message", exception.getMessage()));
    }

    @ExceptionHandler(RenewalException.class)
    public ResponseEntity<Map<String,Object>> handleRenewal(RenewalException exception){return ResponseEntity.status(exception.status()).body(Map.of("code",exception.code(),"message",exception.getMessage()));}
    @ExceptionHandler(MissingGameVersionTransitionException.class)
    public ResponseEntity<Map<String, Object>> handleMissingGameVersionTransition(
            MissingGameVersionTransitionException exception) {
        log.error("Approved game version transition is missing.", exception);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "code", "MISSING_GAME_VERSION_TRANSITION",
                "message", exception.getMessage()));
    }
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

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException exception) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(Map.of(
                "code", "METHOD_NOT_ALLOWED",
                "message", "The HTTP method is not supported for this endpoint."));
    }

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<Map<String, Object>> handleBaseException(HttpServletRequest request, BaseException e) {
        if (e.getCode().getStatusCode() >= HttpStatus.INTERNAL_SERVER_ERROR.value()) {
            log.error("BaseException occurred. requestURL={}, method={}", request.getRequestURL(), request.getMethod(), e);
            notifyError(request, e);
        }
        else {
            log.info("BaseException occurred. requestURL={}, method={}", request.getRequestURL(), request.getMethod(), e);
        }

        return ResponseEntity
                .status(e.getCode().getStatusCode())
                .body(toMap(e));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(
            HttpServletRequest request, Exception exception) {
        log.error("Unexpected server error. path={}, method={}", request.getRequestURI(), request.getMethod(), exception);
        notifyError(request, exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "code", "INTERNAL_SERVER_ERROR", "message", "An unexpected server error occurred."));
    }

    private void notifyError(HttpServletRequest request, Exception exception) {
        errorNotification.notifyServerError(request.getMethod(), request.getRequestURI(),
                exception.getClass().getSimpleName(), request.getHeader("X-Request-Id"));
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
