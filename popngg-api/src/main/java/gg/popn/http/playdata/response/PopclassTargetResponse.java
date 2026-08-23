package gg.popn.http.playdata.response;

import gg.popn.application.playdata.dto.result.PlaydataQueryResults;
import gg.popn.application.playdata.service.PopclassPolicy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
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
        BigDecimal value
) {
    private static final String BANNER_BASE_URL = "https://static.popn.gg/";
    private static final PopclassPolicy POPCLASS_POLICY = new PopclassPolicy();

    public static PopclassTargetResponse current(
            PlaydataQueryResults.ChartPlaydata target) {
        return from(target, target.versionBest(), BigDecimal.valueOf(
                target.popclass() == null ? 0 : target.popclass()));
    }

    public static PopclassTargetResponse potential(
            PlaydataQueryResults.ChartPlaydata target) {
        return from(target, target.allTimeBest(), BigDecimal.valueOf(
                target.popclass() == null ? 0 : target.popclass()));
    }

    public static PopclassTargetResponse legacy(
            PlaydataQueryResults.ChartPlaydata target) {
        return from(target, target.allTimeBest(), BigDecimal.valueOf(
                target.popclass() == null ? 0 : target.popclass()));
    }

    private static PopclassTargetResponse from(
            PlaydataQueryResults.ChartPlaydata target,
            PlaydataQueryResults.Best best,
            BigDecimal value) {
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
                value);
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
            List<PlaydataQueryResults.ChartPlaydata> selected = popclass.targets().stream()
                    .filter(target -> "CURRENT_VERSION".equals(target.popclassBucket())
                            || "OLD_VERSION".equals(target.popclassBucket()))
                    .toList();
            List<PopclassTargetResponse> responses = currentFormulaTargets(selected, potential);
            return new CurrentTargets(
                    bucket(selected, responses, "CURRENT_VERSION"),
                    bucket(selected, responses, "OLD_VERSION"));
        }

        private static List<PopclassTargetResponse> currentFormulaTargets(
                List<PlaydataQueryResults.ChartPlaydata> targets, boolean potential) {
            if (targets.isEmpty()) return List.of();
            List<Long> pointHundredths = targets.stream().map(target -> {
                var best = potential ? target.allTimeBest() : target.versionBest();
                return POPCLASS_POLICY.newChartPointHundredths(
                        target.level(), best.score(), target.medal().code());
            }).toList();
            long officialTotalHundredths = pointHundredths.stream()
                    .mapToLong(Long::longValue).sum() / 60L;
            List<BigDecimal> values = pointHundredths.stream()
                    .map(value -> BigDecimal.valueOf(value)
                            .divide(BigDecimal.valueOf(60), 10, RoundingMode.DOWN))
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
            BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal desired = BigDecimal.valueOf(officialTotalHundredths);
            BigDecimal adjustment = desired.subtract(sum)
                    .divide(BigDecimal.valueOf(values.size()), 10, RoundingMode.DOWN);
            values.replaceAll(value -> value.add(adjustment));
            BigDecimal adjustedSum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            values.set(0, values.getFirst().add(desired.subtract(adjustedSum)));

            List<PopclassTargetResponse> responses = new ArrayList<>(targets.size());
            for (int index = 0; index < targets.size(); index++) {
                var target = targets.get(index);
                var best = potential ? target.allTimeBest() : target.versionBest();
                responses.add(PopclassTargetResponse.from(target, best, values.get(index)));
            }
            return List.copyOf(responses);
        }

        private static List<PopclassTargetResponse> bucket(
                List<PlaydataQueryResults.ChartPlaydata> targets,
                List<PopclassTargetResponse> responses,
                String bucket) {
            List<PopclassTargetResponse> result = new ArrayList<>();
            for (int index = 0; index < targets.size(); index++) {
                if (bucket.equals(targets.get(index).popclassBucket())) {
                    result.add(responses.get(index));
                }
            }
            return List.copyOf(result);
        }
    }
}
