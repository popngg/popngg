package gg.popn.application.playdata.service;

import gg.popn.application.playdata.dto.command.ImportPlaydataCommand;
import gg.popn.application.playdata.dto.result.ImportPlaydataResult;
import gg.popn.application.playdata.exception.DuplicatePlaydataRowIdentityException;
import gg.popn.application.playdata.port.in.ImportPlaydataUseCase;
import gg.popn.application.playdata.port.out.PlaydataImportPort;
import gg.popn.application.playdata.port.out.UnknownChartNotifier;
import gg.popn.application.playdata.port.out.UnknownChartReportPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ImportPlaydataService implements ImportPlaydataUseCase {
    private final PlaydataImportPort importPort;
    private final UnknownChartNotifier unknownChartNotifier;
    private final UnknownChartReportPort unknownChartReportPort;

    @Override
    public ImportPlaydataResult importPlaydata(ImportPlaydataCommand command) {
        if (command.poptomoId() == null || command.poptomoId().isBlank()) {
            throw new IllegalArgumentException("poptomoId is required.");
        }
        if (command.rows() == null || command.rows().isEmpty()) {
            throw new IllegalArgumentException("At least one playdata row is required.");
        }
        var identities = new HashSet<RowIdentity>();
        for (var row : command.rows()) {
            validateRow(row);
            RowIdentity identity = row.chartId() != null
                    ? new RowIdentity(row.chartId(), null, null, null, null, null, null, null)
                    : new RowIdentity(null, row.songId(), row.difficultyCode(), row.upper(),
                    row.songHash(), row.songName(), row.genreName(), row.artistName());
            if (!identities.add(identity)) {
                throw new DuplicatePlaydataRowIdentityException();
            }
        }
        ImportPlaydataResult result = importPort.execute(command);
        List<ImportPlaydataCommand.Row> unknownRows = result.unmatched().stream()
                .filter(row -> "CHART_NOT_FOUND".equals(row.reason()))
                .map(row -> command.rows().get(row.rowIndex()))
                .toList();
        if (!unknownRows.isEmpty()) {
            unknownChartReportPort.record(result.renewLogId(), command.poptomoId(), unknownRows);
            unknownChartNotifier.notifyUnknownCharts(
                    result.renewLogId(), command.poptomoId(), unknownRows);
        }
        return result;
    }

    private static void validateRow(ImportPlaydataCommand.Row row) {
        if (row.score() == null || row.score() < 0 || row.score() > 100_000) {
            throw new IllegalArgumentException("score must be between 0 and 100000.");
        }
        if (row.rankCode() == null || row.medalCode() == null) {
            throw new IllegalArgumentException("rankCode and medalCode are required.");
        }
        boolean hasChartId = row.chartId() != null;
        boolean hasSongDifficulty = row.songId() != null && row.difficultyCode() != null && row.upper() != null;
        boolean hasAlias = row.songHash() != null && !row.songHash().isBlank()
                && row.difficultyCode() != null && row.upper() != null;
        boolean hasMetadata = row.songName() != null && !row.songName().isBlank()
                && row.genreName() != null && !row.genreName().isBlank()
                && row.difficultyCode() != null && row.upper() != null;
        if (!(hasChartId || hasSongDifficulty || hasAlias || hasMetadata)) {
            throw new IllegalArgumentException("A supported chart identity is required.");
        }
    }

    private record RowIdentity(Long chartId, Long songId, Integer difficultyCode, Boolean upper,
                               String songHash, String songName, String genreName,
                               String artistName) {
    }
}
