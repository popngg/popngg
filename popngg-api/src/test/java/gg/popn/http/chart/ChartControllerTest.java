package gg.popn.http.chart;

import gg.popn.application.chart.port.in.CreateChartUseCase;
import gg.popn.http.common.exception.BaseExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ChartControllerTest {
    @Test
    void removedReadRoutesAreNotMapped() throws Exception {
        var create = mock(CreateChartUseCase.class);
        var mvc = MockMvcBuilders.standaloneSetup(new ChartController(create))
                .setControllerAdvice(new BaseExceptionHandler()).build();
        for (String path : List.of("/api/v2/chart/all", "/api/v2/chart/recent",
                "/api/v2/chart/song-hash", "/api/v2/chart/song-hash/4")) {
            mvc.perform(get(path)).andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        }
        verifyNoInteractions(create);
    }
}
