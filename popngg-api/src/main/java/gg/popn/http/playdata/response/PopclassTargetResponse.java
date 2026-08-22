package gg.popn.http.playdata.response;

import gg.popn.application.playdata.dto.result.PlaydataQueryResults;

import java.util.List;

public record PopclassTargetResponse(
        String id,
        String title,
        String genre,
        String bannerUrl,
        int difficulty,
        int level,
        int score,
        int medal,
        int rank,
        int version,
        int value
) {
    private static final String BANNER_BASE_URL = "https://static.popn.gg/";

    public static PopclassTargetResponse current(
            PlaydataQueryResults.ChartPlaydata target) {
        return from(target, target.versionBest());
    }

    public static PopclassTargetResponse potential(
            PlaydataQueryResults.ChartPlaydata target) {
        return from(target, target.allTimeBest());
    }

    public static PopclassTargetResponse legacy(
            PlaydataQueryResults.ChartPlaydata target) {
        return from(target, target.allTimeBest());
    }

    private static PopclassTargetResponse from(
            PlaydataQueryResults.ChartPlaydata target,
            PlaydataQueryResults.Best best) {
        return new PopclassTargetResponse(
                Long.toString(target.chartId()),
                target.songName(),
                target.genreName(),
                BANNER_BASE_URL + target.songHash() + ".png",
                target.difficultyCode(),
                target.level(),
                best.score(),
                target.medal().code(),
                best.rankCode() == null ? 13 : best.rankCode(),
                best.gameVersion() == null ? target.chartVersion() : best.gameVersion(),
                target.popclass() == null ? 0 : target.popclass());
    }

    public record CurrentTargets(
            List<PopclassTargetResponse> newSongs,
            List<PopclassTargetResponse> oldSongs
    ) {
        public static CurrentTargets from(PlaydataQueryResults.Popclass popclass) {
            return from(popclass, false);
        }

        public static CurrentTargets potentialFrom(PlaydataQueryResults.Popclass popclass) {
            return from(popclass, true);
        }

        private static CurrentTargets from(
                PlaydataQueryResults.Popclass popclass, boolean potential) {
            return new CurrentTargets(
                    targets(popclass, "CURRENT_VERSION", potential),
                    targets(popclass, "OLD_VERSION", potential));
        }

        private static List<PopclassTargetResponse> targets(
                PlaydataQueryResults.Popclass popclass, String bucket, boolean potential) {
            return popclass.targets().stream()
                    .filter(target -> bucket.equals(target.popclassBucket()))
                    .map(target -> potential
                            ? PopclassTargetResponse.potential(target)
                            : PopclassTargetResponse.current(target))
                    .toList();
        }
    }
}
