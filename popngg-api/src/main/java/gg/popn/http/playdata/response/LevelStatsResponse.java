package gg.popn.http.playdata.response;

import gg.popn.application.playdata.dto.result.PlaydataQueryResults;

import java.util.List;

public record LevelStatsResponse(
        int level,
        int total,
        List<PlaydataQueryResults.CodeCount> medals,
        List<PlaydataQueryResults.CodeCount> ranks
) {
    public static LevelStatsResponse from(PlaydataQueryResults.ProgressRow row) {
        return new LevelStatsResponse(
                row.key(), row.total(), row.medals(), row.ranks());
    }
}
