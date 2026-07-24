package gg.popn.http.playdata.response;

import gg.popn.application.playdata.dto.result.ImportPlaydataResult;

import java.util.List;

public record ImportPlaydataResponse(
        long renewLogId,
        int receivedCount,
        int matchedCount,
        int updatedCount,
        int historyCount,
        int skippedCount,
        List<UnmatchedRow> unmatched
) {
    public static ImportPlaydataResponse from(ImportPlaydataResult result) {
        return new ImportPlaydataResponse(result.renewLogId(), result.receivedCount(),
                result.matchedCount(), result.updatedCount(), result.historyCount(),
                result.skippedCount(), result.unmatched().stream()
                .map(row -> new UnmatchedRow(row.rowIndex(), row.reason())).toList());
    }

    public record UnmatchedRow(int rowIndex, String reason) {
    }
}
