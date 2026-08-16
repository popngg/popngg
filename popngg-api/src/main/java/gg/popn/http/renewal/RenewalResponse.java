package gg.popn.http.renewal;
import gg.popn.application.playdata.dto.result.ImportPlaydataResult;
import java.time.Instant; import java.util.List;
public record RenewalResponse(Instant renewedAt,long renewLogId,Summary summary,List<ImportPlaydataResult.UnmatchedRow> unmatched){
    public static RenewalResponse from(ImportPlaydataResult r){return new RenewalResponse(Instant.now(),r.renewLogId(),new Summary(r.receivedCount(),r.matchedCount(),r.updatedCount(),r.historyCount(),r.skippedCount()),r.unmatched());}
    public record Summary(int chartsScanned,int chartsMatched,int recordsUpdated,int historyEvents,int chartsSkipped){}
}
