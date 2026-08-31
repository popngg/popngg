package gg.popn.http.common.exception;

import gg.popn.application.common.ErrorNotificationPort;
import jakarta.servlet.http.HttpServletRequest;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class UnexpectedExceptionHandlerTest {
    @Test
    void returnsAndNotifiesTheSameGeneratedTraceId() {
        AtomicReference<String> notifiedTraceId = new AtomicReference<>();
        ErrorNotificationPort notifier = (method, path, type, message, cause, traceId) ->
                notifiedTraceId.set(traceId);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/failure");

        var response = new BaseExceptionHandler(notifier)
                .handleUnexpected(request, new IllegalStateException("broken"));

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).containsEntry("traceId", notifiedTraceId.get());
        assertThat(notifiedTraceId.get()).matches("[0-9a-f-]{36}");
    }

    @Test
    void preservesAValidRequestId() {
        AtomicReference<String> notifiedTraceId = new AtomicReference<>();
        ErrorNotificationPort notifier = (method, path, type, message, cause, traceId) ->
                notifiedTraceId.set(traceId);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/failure");
        when(request.getHeader("X-Request-Id")).thenReturn("edge-request-42");

        var response = new BaseExceptionHandler(notifier)
                .handleUnexpected(request, new IllegalArgumentException("broken"));

        assertThat(response.getBody()).containsEntry("traceId", "edge-request-42");
        assertThat(notifiedTraceId.get()).isEqualTo("edge-request-42");
    }
}
