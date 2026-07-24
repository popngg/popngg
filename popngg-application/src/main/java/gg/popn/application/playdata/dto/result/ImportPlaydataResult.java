package gg.popn.application.playdata.dto.result;

import java.util.List;

public record ImportPlaydataResult(
        long renewLogId,
        int receivedCount,
        int matchedCount,
        int updatedCount,
        int historyCount,
        int skippedCount,
        List<UnmatchedRow> unmatched
) {
    public record UnmatchedRow(int rowIndex, String reason) {
    }
}
