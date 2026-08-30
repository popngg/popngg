package gg.popn.application.playdata.port.out;

import gg.popn.application.playdata.dto.command.ImportPlaydataCommand;
import java.time.Instant;
import java.util.List;

public interface UnknownChartReportPort {
    void record(long renewLogId, String poptomoId, List<ImportPlaydataCommand.Row> rows);
    List<Report> findRecentUnresolved(int limit);
    List<IncompleteReport> findRecentIncomplete(int limit);
    void resolve(long reportId);

    record Report(long reportId, String songName, String genreName, String artistName,
                  int occurrences, Instant lastSeenAt) {}
    record IncompleteReport(long reportId, long songId, String songName, String genreName,
                            String reportedArtistName, String registeredArtistName,
                            int occurrences, Instant lastSeenAt) {}
}
