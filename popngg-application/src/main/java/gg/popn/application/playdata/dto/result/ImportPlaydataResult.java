package gg.popn.application.playdata.dto.result;

import java.util.List;

public record ImportPlaydataResult(
        long renewLogId,
        int receivedCount,
        int matchedCount,
        int updatedCount,
        int historyCount,
        int skippedCount,
        int recordsAdded,
        int medalsImproved,
        int scoresImproved,
        Integer popnClassDelta,
        List<UnmatchedRow> unmatched
) {
    public ImportPlaydataResult(long renewLogId, int receivedCount, int matchedCount,
                                int updatedCount, int historyCount, int skippedCount,
                                List<UnmatchedRow> unmatched) {
        this(renewLogId, receivedCount, matchedCount, updatedCount, historyCount,
                skippedCount, 0, 0, 0, null, unmatched);
    }

    public record UnmatchedRow(int rowIndex, String reason) {
    }
}
