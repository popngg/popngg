package gg.popn.application.playdata.service;

import gg.popn.application.playdata.dto.result.PopclassRecalculationResult;
import gg.popn.application.playdata.port.out.PopclassRecalculationPort;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PopclassRecalculationServiceTest {
    private final PopclassRecalculationPort port = mock(PopclassRecalculationPort.class);
    private final PopclassRecalculationService service =
            new PopclassRecalculationService(port);

    @Test
    void delegatesAuthenticatedUserRecalculation() {
        var expected = new PopclassRecalculationResult(
                "0000", 196, 2_982, 2_982, 1_000, 1, 0);
        when(port.recalculate("0000")).thenReturn(expected);

        assertThat(service.recalculate("0000")).isSameAs(expected);
    }

    @Test
    void rejectsBlankPoptomoId() {
        assertThatThrownBy(() -> service.recalculate(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("poptomoId");
    }
}
