package gg.popn.http.common.exception;

import gg.popn.application.common.ErrorNotificationPort;
import gg.popn.application.song.exception.InvalidSongQueryException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class InvalidSongQueryExceptionHandlerTest {

    @Test
    void treatsAnInvalidSongPageSizeAsAClientErrorWithoutServerNotification() {
        ErrorNotificationPort notifier = mock(ErrorNotificationPort.class);

        var response = new BaseExceptionHandler(notifier)
                .handleInvalidSongQuery(new InvalidSongQueryException(
                        "size must be between 1 and 100"));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody())
                .containsEntry("code", "INVALID_SONG_QUERY")
                .containsEntry("message", "size must be between 1 and 100");
        verifyNoInteractions(notifier);
    }
}
