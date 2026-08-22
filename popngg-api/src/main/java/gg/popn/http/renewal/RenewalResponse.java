package gg.popn.http.renewal;

import gg.popn.application.playdata.dto.result.ImportPlaydataResult;

import java.time.Instant;
import java.util.List;

public record RenewalResponse(
        Instant renewedAt,
        long renewLogId,
        Summary summary,
        List<ImportPlaydataResult.UnmatchedRow> unmatched
) {
    public static RenewalResponse from(ImportPlaydataResult result) {
        return new RenewalResponse(Instant.now(), result.renewLogId(), new Summary(
                result.receivedCount(), result.recordsAdded(), result.medalsImproved(),
                result.scoresImproved(), result.popnClassDelta(), result.matchedCount(),
                result.updatedCount(), result.historyCount(), result.skippedCount()),
                result.unmatched());
    }

    public record Summary(
            int chartsScanned,
            int recordsAdded,
            int medalsImproved,
            int scoresImproved,
            Integer popnClassDelta,
            int chartsMatched,
            int recordsUpdated,
            int historyEvents,
            int chartsSkipped
    ) {
    }
}
