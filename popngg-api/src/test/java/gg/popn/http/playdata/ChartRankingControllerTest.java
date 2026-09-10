package gg.popn.http.playdata;

import gg.popn.application.playdata.dto.result.PlaydataQueryResults;
import gg.popn.application.playdata.port.in.PlaydataQueryUseCase;
import gg.popn.application.song.dto.result.ChartMetadataView;
import gg.popn.application.song.dto.result.DifficultyView;
import gg.popn.application.song.dto.result.SongDetailView;
import gg.popn.application.song.port.in.FindSongDetailUseCase;
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
    private static final String HASH = "0544caf88393e7028ffe914449a57541febc230d373723bafc62bb0a0df29130";
    private final FindSongDetailUseCase songs = mock(FindSongDetailUseCase.class);
    private final PlaydataQueryUseCase rankings = mock(PlaydataQueryUseCase.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new ChartRankingController(songs, rankings))
            .setControllerAdvice(new BaseExceptionHandler()).build();

    @ParameterizedTest
    @CsvSource({"easy,1", "e,1", "normal,2", "n,2", "hyper,3", "h,3", "ex,4", "EX,4"})
    void resolvesHashAndDifficulty(String difficulty, int code) throws Exception {
        when(songs.findSong(HASH)).thenReturn(new SongDetailView(null,
                List.of(chart(99, code, true), chart(42, code, false), chart(43, code == 4 ? 3 : 4, false))));
        var entry = new PlaydataQueryResults.RankingEntry(1, "1234", "player", 10, 99000, 1, 2, 29);
        when(rankings.findChartRankings(42, 100)).thenReturn(
                new PlaydataQueryResults.ChartRankings(42, List.of(entry), List.of(entry)));
        mvc.perform(get("/api/v1/charts/{hash}/{difficulty}/rankings", HASH, difficulty))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.chartId").value(42))
                .andExpect(jsonPath("$.data.currentVersion[0].score").value(99000))
                .andExpect(jsonPath("$.data.allTime[0].ranking").value(1));
        verify(rankings).findChartRankings(42, 100);
    }

    @Test
    void forwardsExplicitLimit() throws Exception {
        when(songs.findSong(HASH)).thenReturn(new SongDetailView(null, List.of(chart(42, 4, false))));
        when(rankings.findChartRankings(42, 20)).thenReturn(new PlaydataQueryResults.ChartRankings(42, List.of(), List.of()));
        mvc.perform(get("/api/v1/charts/{hash}/ex/rankings", HASH).param("limit", "20"))
                .andExpect(status().isOk());
        verify(rankings).findChartRankings(42, 20);
    }

    @ParameterizedTest
    @CsvSource({"ex,0", "ex,101", "invalid,10"})
    void rejectsInvalidParameters(String difficulty, int limit) throws Exception {
        mvc.perform(get("/api/v1/charts/{hash}/{difficulty}/rankings", HASH, difficulty)
                        .param("limit", Integer.toString(limit)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(songs, rankings);
    }

    @Test
    void unknownHashReturnsNotFound() throws Exception {
        when(songs.findSong(HASH)).thenThrow(
                new gg.popn.application.song.exception.CatalogItemNotFoundException("Song", HASH));
        mvc.perform(get("/api/v1/charts/{hash}/ex/rankings", HASH)).andExpect(status().isNotFound());
        verifyNoInteractions(rankings);
    }

    @Test
    void missingOrDeletedDifficultyReturnsNotFound() throws Exception {
        when(songs.findSong(HASH)).thenReturn(new SongDetailView(null,
                List.of(chart(42, 4, true), chart(43, 3, false))));
        mvc.perform(get("/api/v1/charts/{hash}/ex/rankings", HASH)).andExpect(status().isNotFound());
        verifyNoInteractions(rankings);
    }

    private static ChartMetadataView chart(long id, int code, boolean deleted) {
        return new ChartMetadataView(id, new DifficultyView(code, "", "", code),
                48, 29, false, false, false, deleted);
    }
}
