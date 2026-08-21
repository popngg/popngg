package gg.popn.http.common.exception;

import gg.popn.application.playdata.service.PlaydataUpsertPolicy.MissingGameVersionTransitionException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VersionTransitionExceptionHandlerTest {
    @Test
    void returnsConflictWithTheActualTransitionError() {
        var response = new BaseExceptionHandler()
                .handleMissingGameVersionTransition(
                        new MissingGameVersionTransitionException(28, 29));

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody())
                .containsEntry("code", "MISSING_GAME_VERSION_TRANSITION")
                .containsEntry("message",
                        "No approved game version transition exists from 28 to 29.");
    }
}
