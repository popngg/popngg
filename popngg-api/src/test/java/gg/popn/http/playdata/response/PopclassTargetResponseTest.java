package gg.popn.http.playdata.response;

import gg.popn.application.playdata.dto.result.PlaydataQueryResults;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PopclassTargetResponseTest {
    @Test
    void mapsCurrentTargetsIntoFrontendBucketsAndFields() {
        var current = target("CURRENT_VERSION", 1, 13, 13);
        var old = target("OLD_VERSION", 2, 12, 5);
        var response = PopclassTargetResponse.CurrentTargets.from(
                new PlaydataQueryResults.Popclass(
                        "0000", "user", 1, 2, 3, List.of(current, old)));

        assertThat(response.newSongs()).containsExactly(
                new PopclassTargetResponse(
                        "100", "song", "genre",
                        "https://static.popn.gg/hash.png",
                        4, 48, 97_000, 13, 2, 28, 2_500));
        assertThat(response.oldSongs()).hasSize(1);
        assertThat(response.oldSongs().getFirst().medal()).isEqualTo(12);
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
        return new PlaydataQueryResults.ChartPlaydata(
                100, "hash", "genre", "song", 4, "EX", 48, 29, false,
                new PlaydataQueryResults.Best(90_000, versionRank, 29),
                new PlaydataQueryResults.Best(97_000, 2, 28),
                new PlaydataQueryResults.Medal(medal),
                2_500, bucket, bucketRank);
    }
}
