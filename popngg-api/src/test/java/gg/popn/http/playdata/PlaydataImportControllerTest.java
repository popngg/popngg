package gg.popn.http.playdata;

import gg.popn.application.playdata.dto.result.ImportPlaydataResult;
import gg.popn.application.playdata.port.in.ImportPlaydataUseCase;
import gg.popn.domain.user.model.field.PoptomoId;
import gg.popn.domain.user.model.field.UserRole;
import gg.popn.http.playdata.request.ImportPlaydataRequest;
import gg.popn.infra.security.CustomUserPrincipal;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlaydataImportControllerTest {
    private final ImportPlaydataUseCase useCase = mock(ImportPlaydataUseCase.class);
    private final PlaydataImportController controller = new PlaydataImportController(useCase);

    @Test
    void mapsAuthenticatedRequestAndResult() {
        var request = new ImportPlaydataRequest(null, List.of(
                new ImportPlaydataRequest.Row(1L, null, null, null,
                        null, null, null, 90_000, 2, 3)));
        when(useCase.importPlaydata(argThat(command ->
                command.poptomoId().equals("0000-0000-0000"))))
                .thenReturn(new ImportPlaydataResult(8, 1, 1, 0, 0, 0, List.of()));
        var principal = new CustomUserPrincipal(
                PoptomoId.of("0000-0000-0000"), UserRole.from("USER"));

        var response = controller.importPlaydata(principal, request);

        assertThat(response.getData().renewLogId()).isEqualTo(8);
        assertThat(response.getData().matchedCount()).isEqualTo(1);
    }

    @Test
    void rejectsMissingAuthentication() {
        var request = new ImportPlaydataRequest(null, List.of(
                new ImportPlaydataRequest.Row(1L, null, null, null,
                        null, null, null, 1, 1, 1)));
        assertThatThrownBy(() -> controller.importPlaydata(null, request))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
