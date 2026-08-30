package gg.popn.application.playdata.service;

import gg.popn.application.playdata.dto.query.FindUserRecordsQuery;
import gg.popn.application.playdata.dto.result.PlaydataQueryResults;
import gg.popn.application.playdata.port.out.PlaydataQueryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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

    @Test
    void validatesAndDelegatesFrontendQueries() {
        var query = new FindUserRecordsQuery(null, 29, 1, 50, null, null, null,
                0, 100_000, "level", "desc", 0, 20);
        var records = new PlaydataQueryResults.UserRecords(List.of(), 0, 0, 20);
        var progress = new PlaydataQueryResults.Progress(List.of(),
                new PlaydataQueryResults.ProgressCounts(0, 0, List.of(), List.of()));
        when(port.findUserRecords("0000", new FindUserRecordsQuery(
                null, 29, 1, 50, null, null, null, 0, 100_000,
                "LEVEL", "DESC", 0, 20))).thenReturn(records);
        when(port.findProgress("0000", "DIFFICULTY")).thenReturn(progress);

        assertThat(service.findUserRecords("0000", query)).isSameAs(records);
        assertThat(service.findProgress("0000", "difficulty")).isSameAs(progress);
    }

    @ParameterizedTest
    @ValueSource(strings = {"level", "version", "difficulty", "title", "genre", "score", "medal", "rank"})
    void acceptsEveryUserRecordSort(String sort) {
        var query = new FindUserRecordsQuery(null, null, null, null, null, null, null,
                null, null, sort, "asc", 0, 20);
        var normalized = new FindUserRecordsQuery(null, null, null, null, null, null, null,
                null, null, sort.toUpperCase(), "ASC", 0, 20);
        var records = new PlaydataQueryResults.UserRecords(List.of(), 0, 0, 20);
        when(port.findUserRecords("0000", normalized)).thenReturn(records);

        assertThat(service.findUserRecords("0000", query)).isSameAs(records);
    }

    @Test
    void rejectsUnsupportedUserRecordSort() {
        var query = new FindUserRecordsQuery(null, null, null, null, null, null, null,
                null, null, "artist", "asc", 0, 20);
        assertThatThrownBy(() -> service.findUserRecords("0000", query))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsInvalidFrontendQueryRangesAndPagination() {
        assertThatThrownBy(() -> service.findUserRecords("0000", query(1, 20, 10, 1, 0, 100)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.findUserRecords("0000", query(1, 20, 1, 10, 100, 0)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.findUserRecords("0000", query(-1, 20, 1, 10, 0, 100)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.findUserRecords("0000", query(0, 101, 1, 10, 0, 100)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static FindUserRecordsQuery query(
            int page, int size, int levelMin, int levelMax, int scoreMin, int scoreMax) {
        return new FindUserRecordsQuery(null, null, levelMin, levelMax, null, null, null,
                scoreMin, scoreMax, "level", "desc", page, size);
    }
}
