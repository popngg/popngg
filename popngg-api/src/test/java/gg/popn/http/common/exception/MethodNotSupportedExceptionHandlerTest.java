package gg.popn.http.common.exception;

import gg.popn.application.common.ErrorNotificationPort;
import org.junit.jupiter.api.Test;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class MethodNotSupportedExceptionHandlerTest {
    @Test
    void returnsMethodNotAllowedWithoutSendingServerErrorNotification() {
        AtomicBoolean notified = new AtomicBoolean();
        ErrorNotificationPort notifier = (method, path, exceptionType, traceId) -> notified.set(true);
        var handler = new BaseExceptionHandler(notifier);

        var response = handler.handleMethodNotSupported(
                new HttpRequestMethodNotSupportedException("GET", List.of("POST")));

        assertThat(response.getStatusCode().value()).isEqualTo(405);
        assertThat(response.getBody()).containsEntry("code", "METHOD_NOT_ALLOWED");
        assertThat(notified).isFalse();
    }
}
