package gg.popn.application.playdata.port.out;

import gg.popn.application.playdata.dto.command.ImportPlaydataCommand;

import java.util.List;

public interface UnknownChartNotifier {
    void notifyUnknownCharts(long renewLogId, String poptomoId,
                             List<ImportPlaydataCommand.Row> rows);
}
