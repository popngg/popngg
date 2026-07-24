package gg.popn.application.playdata.port.out;

import gg.popn.application.playdata.dto.result.PlaydataQueryResults;

public interface PlaydataQueryPort {
    PlaydataQueryResults.UserPlaydata findUserPlaydata(String poptomoId);

    PlaydataQueryResults.Counts count(String poptomoId, String groupBy, String target);

    PlaydataQueryResults.Popclass findPopclass(String poptomoId);

    PlaydataQueryResults.ChartRankings findChartRankings(long chartId, int limit);
}
