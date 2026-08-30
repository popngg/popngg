package gg.popn.http.playdata.response;

import gg.popn.application.playdata.dto.result.PlaydataQueryResults;
import gg.popn.application.playdata.service.PopclassPolicy;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PopclassTargetResponseTest {
    @Test
    void mapsCurrentTargetsIntoFrontendBucketsAndFields() {
        var current = target("CURRENT_VERSION", 1, 13, 13);
        var old = target("OLD_VERSION", 2, 12, 5);
        var response = PopclassTargetResponse.CurrentTargets.from(
                new PlaydataQueryResults.Popclass(
                        "0000", "user", 1, 2, 3, List.of(current, old)));

        assertThat(response.newSongs()).hasSize(1);
        assertThat(response.newSongs().getFirst().id()).isEqualTo("100");
        assertThat(response.newSongs().getFirst().score()).isEqualTo(90_000);
        assertThat(response.oldSongs()).hasSize(1);
        assertThat(response.oldSongs().getFirst().medal()).isEqualTo(12);
        var policy = new PopclassPolicy();
        assertThat(response.newSongs().getFirst().value()).isEqualByComparingTo(
                chartValue(policy, 48, 90_000, 13));
        assertThat(response.oldSongs().getFirst().value()).isEqualByComparingTo(
                chartValue(policy, 48, 90_000, 12));
    }

    @Test
    void mapsPotentialTargetsFromAllTimeValues() {
        var response = PopclassTargetResponse.CurrentTargets.potentialFrom(
                new PlaydataQueryResults.Popclass(
                        "0000", "user", 1, 2, 3,
                        List.of(target("CURRENT_VERSION", 1, 3, 5))));

        assertThat(response.newSongs().getFirst().score()).isEqualTo(97_000);
        assertThat(response.newSongs().getFirst().rank()).isEqualTo(2);
        assertThat(response.newSongs().getFirst().version()).isEqualTo(28);
    }

    @Test
    void returnsEachChartValueWithoutRedistributingTheTotalRemainder() {
        var first = target("CURRENT_VERSION", 1, 6, 5, 49, 89_752);
        var second = target("OLD_VERSION", 1, 7, 5, 49, 89_446);
        var response = PopclassTargetResponse.CurrentTargets.potentialFrom(
                new PlaydataQueryResults.Popclass(
                        "0000", "user", 1, 2, 3, List.of(first, second)));

        var policy = new PopclassPolicy();
        assertThat(response.newSongs().getFirst().value()).isEqualByComparingTo(
                chartValue(policy, 49, 89_752, 6));
        assertThat(response.oldSongs().getFirst().value()).isEqualByComparingTo(
                chartValue(policy, 49, 89_446, 7));
    }

    @Test
    void keepsSameScoreValueStableWhenAnotherChartsAllTimeScoreChanges() {
        var stable = targetWithScores(100, "CURRENT_VERSION", 49, 5, 89_752, 89_752);
        var changed = targetWithScores(101, "OLD_VERSION", 49, 5, 90_000, 95_000);
        var popclass = new PlaydataQueryResults.Popclass(
                "0000", "user", 1, 2, 3, List.of(stable, changed));

        var current = PopclassTargetResponse.CurrentTargets.from(popclass);
        var potential = PopclassTargetResponse.CurrentTargets.potentialFrom(popclass);

        assertThat(current.newSongs().getFirst().value())
                .isEqualByComparingTo(potential.newSongs().getFirst().value());
    }

    @Test
    void mapsLegacyTargetsFromAllTimeValuesWithoutRemappingMedalCodes() {
        var response = PopclassTargetResponse.legacy(
                target(null, null, 10, 5));

        assertThat(response.score()).isEqualTo(97_000);
        assertThat(response.rank()).isEqualTo(2);
        assertThat(response.version()).isEqualTo(28);
        assertThat(response.medal()).isEqualTo(10);
    }

    private static PlaydataQueryResults.ChartPlaydata target(
            String bucket, Integer bucketRank, int medal, Integer versionRank) {
        return target(bucket, bucketRank, medal, versionRank, 48, 97_000);
    }

    private static PlaydataQueryResults.ChartPlaydata target(
            String bucket, Integer bucketRank, int medal, Integer versionRank,
            int level, int allTimeScore) {
        return new PlaydataQueryResults.ChartPlaydata(
                100, "hash", "genre", "song", 4, "EX", level, 29, false,
                new PlaydataQueryResults.Best(90_000, versionRank, 29),
                new PlaydataQueryResults.Best(allTimeScore, 2, 28),
                new PlaydataQueryResults.Medal(medal),
                2_500, bucket, bucketRank);
    }

    private static PlaydataQueryResults.ChartPlaydata targetWithScores(
            long chartId, String bucket, int level, int medal,
            int versionScore, int allTimeScore) {
        return new PlaydataQueryResults.ChartPlaydata(
                chartId, "hash", "genre", "song", 4, "EX", level, 29, false,
                new PlaydataQueryResults.Best(versionScore, 2, 29),
                new PlaydataQueryResults.Best(allTimeScore, 2, 28),
                new PlaydataQueryResults.Medal(medal),
                2_500, bucket, 1);
    }

    private static BigDecimal chartValue(
            PopclassPolicy policy, int level, int score, int medal) {
        return BigDecimal.valueOf(policy.newChartPointHundredths(level, score, medal))
                .divide(BigDecimal.valueOf(60), 10, java.math.RoundingMode.DOWN);
    }
}
