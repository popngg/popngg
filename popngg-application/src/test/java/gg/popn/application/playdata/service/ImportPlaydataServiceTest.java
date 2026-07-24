package gg.popn.application.playdata.service;

import gg.popn.application.playdata.dto.command.ImportPlaydataCommand;
import gg.popn.application.playdata.dto.result.ImportPlaydataResult;
import gg.popn.application.playdata.port.out.PlaydataImportPort;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImportPlaydataServiceTest {
    private final PlaydataImportPort port = mock(PlaydataImportPort.class);
    private final ImportPlaydataService service = new ImportPlaydataService(port);

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
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate");
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
