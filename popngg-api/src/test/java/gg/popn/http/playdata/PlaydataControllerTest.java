package gg.popn.http.playdata;

import gg.popn.application.playdata.dto.result.PlaydataQueryResults;
import gg.popn.application.playdata.port.in.PlaydataQueryUseCase;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class PlaydataControllerTest {
    private final PlaydataQueryUseCase useCase = mock(PlaydataQueryUseCase.class);
    private final PlaydataController controller = new PlaydataController(useCase);

    @Test
    void exposesAllQueryResults() {
        var user = new PlaydataQueryResults.UserPlaydata(
                "0000", "user", 1, 2, 3, List.of());
        var counts = new PlaydataQueryResults.Counts(List.of());
        var popclass = new PlaydataQueryResults.Popclass(
                "0000", "user", 1, 2, 3, List.of());
        var rankings = new PlaydataQueryResults.ChartRankings(1, List.of(), List.of());
        var progressRow = new PlaydataQueryResults.ProgressRow(
                48, 2, 90_000,
                List.of(new PlaydataQueryResults.CodeCount(1, 2)),
                List.of(new PlaydataQueryResults.CodeCount(2, 2)));
        var progress = new PlaydataQueryResults.Progress(List.of(progressRow),
                new PlaydataQueryResults.ProgressCounts(0, 0, List.of(), List.of()));
        when(useCase.findUserPlaydata("0000")).thenReturn(user);
        when(useCase.count("0000", "level", "rank")).thenReturn(counts);
        when(useCase.findPopclass("0000")).thenReturn(popclass);
        when(useCase.findPotentialPopclass("0000")).thenReturn(popclass);
        when(useCase.findLegacyPopclassTargets("0000")).thenReturn(List.of());
        when(useCase.findChartRankings(1, 50)).thenReturn(rankings);
        when(useCase.findProgress("0000", "level")).thenReturn(progress);
        when(useCase.findUserRecords(org.mockito.ArgumentMatchers.eq("0000"), any()))
                .thenReturn(new PlaydataQueryResults.UserRecords(List.of(), 0, 0, 20));

        assertThat(controller.findUserPlaydata("0000").getData()).isSameAs(user);
        assertThat(controller.count("0000", "level", "rank").getData()).isSameAs(counts);
        assertThat(controller.findCurrentPopclassTargets("0000").getData().newSongs())
                .isEmpty();
        assertThat(controller.findPotentialPopclassTargets("0000").getData().newSongs())
                .isEmpty();
        assertThat(controller.findLegacyPopclassTargets("0000").getData()).isEmpty();
        assertThat(controller.findChartRankings(1, 50).getData()).isSameAs(rankings);
        assertThat(controller.findProgress("0000", "level").getData()).isSameAs(progress);
        assertThat(controller.findLevelStats("0000").getData()).singleElement()
                .satisfies(stats -> {
                    assertThat(stats.level()).isEqualTo(48);
                    assertThat(stats.total()).isEqualTo(2);
                    assertThat(stats.medals()).containsExactly(
                            new PlaydataQueryResults.CodeCount(1, 2));
                    assertThat(stats.ranks()).containsExactly(
                            new PlaydataQueryResults.CodeCount(2, 2));
                });
        assertThat(controller.findUserRecords("0000", null, null, null, null,
                null, null, null, null, null, "level", "desc", 1, 20)
                .getData().totalItems()).isZero();
    }
}
