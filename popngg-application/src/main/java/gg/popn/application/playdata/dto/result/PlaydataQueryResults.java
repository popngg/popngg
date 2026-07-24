package gg.popn.application.playdata.dto.result;

import java.util.List;

public final class PlaydataQueryResults {
    private PlaydataQueryResults() {
    }

    public record UserPlaydata(
            String poptomoId,
            String userName,
            int displayPopclass,
            int potentialPopclass,
            int legacyPopclass,
            List<ChartPlaydata> playdata
    ) {
    }

    public record ChartPlaydata(
            long chartId,
            String songHash,
            String genreName,
            String songName,
            int difficultyCode,
            String difficultyLabel,
            int level,
            int chartVersion,
            boolean upper,
            Best versionBest,
            Best allTimeBest,
            Medal medal,
            Integer popclass,
            String popclassBucket,
            Integer popclassBucketRank
    ) {
    }

    public record Best(int score, Integer rankCode, Integer gameVersion) {
    }

    public record Medal(int code) {
    }

    public record Counts(List<GroupCount> groups) {
    }

    public record GroupCount(String group, int groupCode, String groupLabel,
                             String target, int targetCode, long count) {
    }

    public record Popclass(
            String poptomoId,
            String userName,
            int displayPopclass,
            int potentialPopclass,
            int legacyPopclass,
            List<ChartPlaydata> targets
    ) {
    }

    public record ChartRankings(long chartId, List<RankingEntry> currentVersion,
                                List<RankingEntry> allTime) {
    }

    public record RankingEntry(
            int ranking,
            String poptomoId,
            String userName,
            int displayPopclass,
            int score,
            Integer rankCode,
            int medalCode,
            Integer scoreVersion
    ) {
    }
}
