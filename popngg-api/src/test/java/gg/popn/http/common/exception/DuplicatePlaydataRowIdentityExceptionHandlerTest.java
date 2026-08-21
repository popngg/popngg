package gg.popn.http.common.exception;

import gg.popn.application.playdata.exception.DuplicatePlaydataRowIdentityException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DuplicatePlaydataRowIdentityExceptionHandlerTest {
    @Test
    void returnsUnprocessableEntityInsteadOfInternalServerError() {
        var response = new BaseExceptionHandler()
                .handleDuplicatePlaydataRowIdentity(new DuplicatePlaydataRowIdentityException());

        assertThat(response.getStatusCode().value()).isEqualTo(422);
        assertThat(response.getBody())
                .containsEntry("code", "DUPLICATE_CHART")
                .containsEntry("message", "The renewal payload contains the same chart more than once.");
    }
}
