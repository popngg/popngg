package gg.popn.http.common.exception;

import gg.popn.application.common.ErrorNotificationPort;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class ConstraintViolationExceptionHandlerTest {

    @Test
    void treatsInvalidPathValuesAsClientErrorsWithoutServerNotification() {
        ErrorNotificationPort notifier = mock(ErrorNotificationPort.class);

        var response = new BaseExceptionHandler(notifier)
                .handleConstraintViolation(new ConstraintViolationException(Set.of()));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).containsEntry(
                "code", "INVALID_REQUEST_PARAMETER");
        verifyNoInteractions(notifier);
    }
}
