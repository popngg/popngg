package gg.popn.http.playdata;

import gg.popn.application.playdata.dto.result.PopclassRecalculationResult;
import gg.popn.application.playdata.port.in.RecalculatePopclassUseCase;
import gg.popn.domain.user.model.field.PoptomoId;
import gg.popn.domain.user.model.field.UserRole;
import gg.popn.infra.security.CustomUserPrincipal;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PopclassRecalculationControllerTest {
    private final RecalculatePopclassUseCase useCase = mock(RecalculatePopclassUseCase.class);
    private final PopclassRecalculationController controller =
            new PopclassRecalculationController(useCase);

    @Test
    void recalculatesAuthenticatedUsersPopclasses() {
        var principal = new CustomUserPrincipal(
                PoptomoId.of("0000-0000-0000"), UserRole.from("USER"));
        var result = new PopclassRecalculationResult(
                "0000-0000-0000", 196, 2_982, 2_982, 1_000, 1, 0);
        when(useCase.recalculate("0000-0000-0000")).thenReturn(result);

        assertThat(controller.recalculate(principal).getData()).isSameAs(result);
    }

    @Test
    void rejectsMissingAuthentication() {
        assertThatThrownBy(() -> controller.recalculate(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Authentication");
    }
}
