package gg.popn.application.playdata.service;

import gg.popn.application.playdata.dto.command.ImportPlaydataCommand;
import gg.popn.application.playdata.dto.result.ImportPlaydataResult;
import gg.popn.application.playdata.exception.DuplicatePlaydataRowIdentityException;
import gg.popn.application.playdata.port.out.PlaydataImportPort;
import gg.popn.application.playdata.port.out.UnknownChartNotifier;
import gg.popn.application.playdata.port.out.UnknownChartReportPort;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

class ImportPlaydataServiceTest {
    private final PlaydataImportPort port = mock(PlaydataImportPort.class);
    private final UnknownChartNotifier notifier = mock(UnknownChartNotifier.class);
    private final UnknownChartReportPort reportPort = mock(UnknownChartReportPort.class);
    private final ImportPlaydataService service = new ImportPlaydataService(port, notifier, reportPort);

    @Test
    void validatesAndDelegatesImport() {
        var command = command(row(1L, null, null, null, null, null, null, 90_000, 2, 3));
        var expected = new ImportPlaydataResult(7, 1, 1, 0, 0, 0, List.of());
        when(port.execute(command)).thenReturn(expected);

        assertThat(service.importPlaydata(command)).isEqualTo(expected);
        verify(port).execute(command);
    }

    @Test
    void acceptsEachFallbackIdentity() {
        var rows = List.of(
                row(null, 2L, 3, false, null, null, null, 1, 1, 1),
                row(null, null, 3, false, "hash", null, null, 1, 1, 1),
                row(null, null, 3, false, null, "song", "genre", 1, 1, 1));
        var command = new ImportPlaydataCommand("0000-0000-0000", null, rows);
        when(port.execute(command)).thenReturn(new ImportPlaydataResult(1, 3, 3, 0, 0, 0, List.of()));

        assertThat(service.importPlaydata(command).matchedCount()).isEqualTo(3);
    }

    @Test
    void rejectsInvalidRequestsWithoutLoggingSensitiveRows() {
        assertThatThrownBy(() -> service.importPlaydata(new ImportPlaydataCommand(" ", null, List.of())))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.importPlaydata(new ImportPlaydataCommand("id", null, List.of())))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.importPlaydata(command(
                row(null, null, null, null, null, null, null, -1, 1, 1))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.importPlaydata(command(
                row(null, null, null, null, null, null, null, 1, null, 1))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.importPlaydata(command(
                row(null, null, null, null, null, null, null, 1, 1, 1))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsDuplicateRowIdentity() {
        var duplicate = row(1L, null, null, null, null, null, null, 1, 1, 1);
        assertThatThrownBy(() -> service.importPlaydata(
                new ImportPlaydataCommand("id", null, List.of(duplicate, duplicate))))
                .isInstanceOf(DuplicatePlaydataRowIdentityException.class);
    }

    @Test
    void treatsSameMetadataWithDifferentArtistsAsDifferentFallbackIdentities() {
        var rows = List.of(
                rowWithArtist("artist one"),
                rowWithArtist("artist two"));
        var command = new ImportPlaydataCommand("id", null, rows);
        var expected = new ImportPlaydataResult(1, 2, 2, 0, 0, 0, List.of());
        when(port.execute(command)).thenReturn(expected);

        assertThat(service.importPlaydata(command)).isEqualTo(expected);
    }

    @Test
    void notifiesOnlyRowsThatWereNotFound() {
        var first = row(null, null, 3, false, null, "new song", "genre", 1, 1, 1);
        var second = row(null, null, 4, false, null, "ambiguous", "genre", 1, 1, 1);
        var command = new ImportPlaydataCommand("id", null, List.of(first, second));
        var result = new ImportPlaydataResult(9, 2, 0, 0, 0, 2,
                List.of(new ImportPlaydataResult.UnmatchedRow(0, "CHART_NOT_FOUND"),
                        new ImportPlaydataResult.UnmatchedRow(1, "AMBIGUOUS_CHART")));
        when(port.execute(command)).thenReturn(result);

        service.importPlaydata(command);

        verify(notifier).notifyUnknownCharts(9, "id", List.of(first));
        verify(reportPort).record(9, "id", List.of(first));
    }

    @Test
    void doesNotNotifyWhenEveryChartMatches() {
        var command = command(row(1L, null, null, null, null, null, null, 1, 1, 1));
        when(port.execute(command)).thenReturn(
                new ImportPlaydataResult(1, 1, 1, 0, 0, 0, List.of()));

        service.importPlaydata(command);

        verify(notifier, never()).notifyUnknownCharts(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyList());
    }

    private static ImportPlaydataCommand.Row rowWithArtist(String artist) {
        return new ImportPlaydataCommand.Row(null, null, 3, false, null,
                "same title", "same genre", 1, 1, 1, null, false, artist);
    }

    private static ImportPlaydataCommand command(ImportPlaydataCommand.Row row) {
        return new ImportPlaydataCommand("0000-0000-0000", null, List.of(row));
    }

    private static ImportPlaydataCommand.Row row(Long chartId, Long songId, Integer difficulty,
                                                  Boolean upper, String hash, String song,
                                                  String genre, Integer score, Integer rank,
                                                  Integer medal) {
        return new ImportPlaydataCommand.Row(chartId, songId, difficulty, upper, hash,
                song, genre, score, rank, medal);
    }
}
