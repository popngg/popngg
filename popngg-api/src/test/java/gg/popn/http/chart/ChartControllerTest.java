package gg.popn.http.chart;

import gg.popn.application.chart.port.in.*;
import gg.popn.application.chart.port.out.ChartQueryPort;
import gg.popn.application.chart.service.FindGroupedChartListService;
import gg.popn.application.chart.service.FindGroupedChartListRecentService;
import gg.popn.domain.chart.model.Chart;
import gg.popn.domain.chart.model.field.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ChartControllerTest {
    @Test
    void serializesVersion29OnBothListRoutes() throws Exception {
        var chart = Chart.builder().songHash(SongHash.of("high-cheers-song"))
                .genreName(GenreName.of("genre")).songName(SongName.of("song"))
                .version(Version.of(29)).difficulty(Difficulty.of(4))
                .level(Level.of(49)).isUpper(IsUpper.of(0)).build();
        var query = mock(ChartQueryPort.class);
        when(query.findAllCharts()).thenReturn(List.of(chart));
        when(query.findRecentCharts(5)).thenReturn(List.of(chart));
        var controller = new ChartController(new FindGroupedChartListService(query),
                new FindGroupedChartListRecentService(query), mock(FindChartUseCase.class),
                mock(CreateChartUseCase.class), mock(FindGroupedChartUseCase.class));
        var mvc = MockMvcBuilders.standaloneSetup(controller).build();
        for (String path : List.of("/api/v2/chart/all", "/api/v2/chart/recent")) {
            mvc.perform(get(path)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.groupedCharts[0].exChart.version").value(29))
                    .andExpect(jsonPath("$.data.groupedCharts[0].exChart.level").value(49));
        }
    }
}
