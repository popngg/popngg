package gg.popn.application.playdata.service;

import gg.popn.application.playdata.dto.command.ImportPlaydataCommand;
import gg.popn.application.playdata.dto.result.ImportPlaydataResult;
import gg.popn.application.playdata.port.in.ImportPlaydataUseCase;
import gg.popn.application.playdata.port.out.PlaydataImportPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;

@Service
@RequiredArgsConstructor
public class ImportPlaydataService implements ImportPlaydataUseCase {
    private final PlaydataImportPort importPort;

    @Override
    public ImportPlaydataResult importPlaydata(ImportPlaydataCommand command) {
        if (command.poptomoId() == null || command.poptomoId().isBlank()) {
            throw new IllegalArgumentException("poptomoId is required.");
        }
        if (command.rows() == null || command.rows().isEmpty()) {
            throw new IllegalArgumentException("At least one playdata row is required.");
        }
        var identities = new HashSet<String>();
        for (var row : command.rows()) {
            validateRow(row);
            String identity = row.chartId() != null
                    ? "chart:" + row.chartId()
                    : String.join(":", "fallback", String.valueOf(row.songId()),
                    String.valueOf(row.difficultyCode()), String.valueOf(row.upper()),
                    String.valueOf(row.songHash()), String.valueOf(row.songName()));
            if (!identities.add(identity)) {
                throw new IllegalArgumentException("Duplicate playdata row identity.");
            }
        }
        return importPort.execute(command);
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
}
