package gg.popn.application.playdata.port.out;

import gg.popn.application.playdata.dto.command.ImportPlaydataCommand;
import java.time.Instant;
import java.util.List;

public interface UnknownChartReportPort {
    void record(long renewLogId, String poptomoId, List<ImportPlaydataCommand.Row> rows);
    List<Report> findRecentUnresolved(int limit);

    record Report(long reportId, String songName, String genreName, String artistName,
                  int difficultyCode, boolean upper, int occurrences, Instant lastSeenAt) {}
}
