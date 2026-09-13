package gg.popn.http.playdata;

import gg.popn.application.playdata.dto.result.PlaydataQueryResults;
import gg.popn.application.playdata.port.out.PlaydataQueryPort;
import gg.popn.application.playdata.service.PlaydataQueryService;
import gg.popn.application.playdata.exception.ChartNotFoundException;
import gg.popn.http.common.exception.BaseExceptionHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ChartRankingControllerTest {
    private final PlaydataQueryPort port = mock(PlaydataQueryPort.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(
            new ChartRankingController(new PlaydataQueryService(port)))
            .setControllerAdvice(new BaseExceptionHandler()).build();

    @Test
    void exposesExactContractWithDefaults() throws Exception {
        var entry = new PlaydataQueryResults.ChartRankingEntry(1, "1234-5678-9012", "ABC", null,
                19000, 17000, 100000, 1, 1);
        when(port.findChartRankings("hash", 4, "SCORE", 1, 20)).thenReturn(
                new PlaydataQueryResults.ChartRankingsPage(List.of(entry), 100));
        mvc.perform(get("/api/v1/charts/hash/4/rankings"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"code":"SUCCESS","message":"The request is successful.","data":{
                          "items":[{"position":1,"id":"1234-5678-9012","name":"ABC","avatarUrl":null,
                          "userPopnClass":19000,"popnClass":17000,"score":100000,"rank":1,"medal":1}],
                          "totalItems":100,"totalPages":5,"hasPrev":false,"hasNext":true}}
                        """, true));
    }

    @ParameterizedTest
    @CsvSource({"score,20", "medal,50", "medal,100"})
    void forwardsAxisAndPagination(String axis, int size) throws Exception {
        when(port.findChartRankings("hash", 1, axis.toUpperCase(), 2, size)).thenReturn(
                new PlaydataQueryResults.ChartRankingsPage(List.of(), 0));
        mvc.perform(get("/api/v1/charts/hash/1/rankings").param("axis", axis)
                        .param("page", "2").param("size", Integer.toString(size)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.hasPrev").value(true))
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andExpect(jsonPath("$.data.totalPages").value(0));
        verify(port).findChartRankings("hash", 1, axis.toUpperCase(), 2, size);
    }

    @ParameterizedTest
    @CsvSource({"0,score,1,20", "5,score,1,20", "4,invalid,1,20", "4,score,0,20",
            "4,score,1,0", "4,score,1,101", "ex,score,1,20"})
    void rejectsInvalidParameters(String difficulty, String axis, String page, String size) throws Exception {
        mvc.perform(get("/api/v1/charts/hash/{difficulty}/rankings", difficulty)
                        .param("axis", axis).param("page", page).param("size", size))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(port);
    }

    @Test
    void missingChartReturnsExact404() throws Exception {
        when(port.findChartRankings("hash", 4, "SCORE", 1, 20)).thenThrow(new ChartNotFoundException());
        mvc.perform(get("/api/v1/charts/hash/4/rankings"))
                .andExpect(status().isNotFound())
                .andExpect(content().json("""
                        {"code":"NOT_FOUND","message":"Chart not found.","data":null}
                        """, true));
    }
}
