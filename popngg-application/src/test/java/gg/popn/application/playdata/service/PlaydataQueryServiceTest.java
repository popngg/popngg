package gg.popn.application.playdata.service;

import gg.popn.application.playdata.dto.result.PlaydataQueryResults;
import gg.popn.application.playdata.port.out.PlaydataQueryPort;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlaydataQueryServiceTest {
    private final PlaydataQueryPort port = mock(PlaydataQueryPort.class);
    private final PlaydataQueryService service = new PlaydataQueryService(port);

    @Test
    void normalizesCountOptionsAndDelegates() {
        var expected = new PlaydataQueryResults.Counts(List.of());
        when(port.count("0000", "LEVEL", "MEDAL")).thenReturn(expected);

        assertThat(service.count("0000", "level", "medal")).isSameAs(expected);
        verify(port).count("0000", "LEVEL", "MEDAL");
    }

    @Test
    void rejectsUnsupportedOptionsAndLimits() {
        assertThatThrownBy(() -> service.count("0000", "song", "rank"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.findChartRankings(1, 101))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void delegatesUserPopclassAndRankingQueries() {
        var user = new PlaydataQueryResults.UserPlaydata(
                "0000", "user", 1, 2, 3, List.of());
        var popclass = new PlaydataQueryResults.Popclass(
                "0000", "user", 1, 2, 3, List.of());
        var rankings = new PlaydataQueryResults.ChartRankings(1, List.of(), List.of());
        when(port.findUserPlaydata("0000")).thenReturn(user);
        when(port.findPopclass("0000")).thenReturn(popclass);
        when(port.findLegacyPopclassTargets("0000")).thenReturn(List.of());
        when(port.findChartRankings(1, 10)).thenReturn(rankings);

        assertThat(service.findUserPlaydata("0000")).isSameAs(user);
        assertThat(service.findPopclass("0000")).isSameAs(popclass);
        assertThat(service.findLegacyPopclassTargets("0000")).isEmpty();
        assertThat(service.findChartRankings(1, 10)).isSameAs(rankings);
    }
}
