package gg.popn.application.playdata.service;

import org.junit.jupiter.api.Test;

import static gg.popn.application.playdata.service.PlaydataUpsertPolicy.TransitionPolicy.CARRY_OVER;
import static gg.popn.application.playdata.service.PlaydataUpsertPolicy.TransitionPolicy.RESET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlaydataUpsertPolicyTest {
    private final PlaydataUpsertPolicy policy = new PlaydataUpsertPolicy();

    @Test
    void createsSeparatedVersionAndAllTimeState() {
        var decision = policy.decide(null, observation(90_000, 2, 3), 29, null);

        assertThat(decision.changed()).isTrue();
        assertThat(decision.state()).isEqualTo(state(29, 90_000, 2,
                90_000, 29, 2, 3));
    }

    @Test
    void onlyRaisesScoresButAlwaysKeepsObservedMedalAndPairedRank() {
        var existing = state(29, 95_000, 1, 97_000, 28, 1, 2);

        var lower = policy.decide(existing, observation(90_000, 4, 5), 29, null);
        assertThat(lower.state()).isEqualTo(state(29, 95_000, 1,
                97_000, 28, 1, 5));

        var higher = policy.decide(existing, observation(98_000, 3, 6), 29, null);
        assertThat(higher.state()).isEqualTo(state(29, 98_000, 3,
                98_000, 29, 3, 6));
    }

    @Test
    void doesNotCalculateOrReplaceRankForLowerObservedScore() {
        var existing = state(29, 95_000, 7, 97_000, 28, 8, 3);
        var decision = policy.decide(existing, observation(90_000, 1, 3), 29, null);

        assertThat(decision.changed()).isFalse();
        assertThat(decision.state().versionRankCode()).isEqualTo(7);
        assertThat(decision.state().allTimeRankCode()).isEqualTo(8);
    }

    @Test
    void authoritativeVersionBestReplacesHigherPollutedScore() {
        var existing = state(29, 91_490, 5, 91_490, 29, 5, 6);
        var observed = new PlaydataUpsertPolicy.Observation(
                91_490, 5, 6, 90_727, true);

        var decision = policy.decide(existing, observed, 29, null);

        assertThat(decision.state()).isEqualTo(state(
                29, 90_727, 5, 91_490, 29, 5, 6));
    }

    @Test
    void requiresApprovedTransitionWhenVersionChanges() {
        var existing = state(28, 95_000, 1, 97_000, 28, 1, 2);

        assertThatThrownBy(() -> policy.decide(
                existing, observation(90_000, 3, 4), 29, null))
                .isInstanceOf(PlaydataUpsertPolicy.MissingGameVersionTransitionException.class);
    }

    @Test
    void appliesResetAndCarryOverPolicies() {
        var existing = state(28, 95_000, 1, 97_000, 28, 1, 2);

        assertThat(policy.decide(existing, observation(90_000, 3, 4), 29, RESET)
                .state()).isEqualTo(state(29, 90_000, 3,
                90_000, 29, 3, 4));
        assertThat(policy.decide(existing, observation(90_000, 3, 4), 29, CARRY_OVER)
                .state()).isEqualTo(state(29, 95_000, 1,
                97_000, 28, 1, 4));
    }

    @Test
    void replacesOldAllTimeScoreWithAuthoritativeTransferResultOnReset() {
        var scoreRecordedAfterTransfer = state(28, 98_000, 1,
                98_000, 28, 1, 2);

        var decision = policy.decide(scoreRecordedAfterTransfer,
                observation(90_000, 3, 4), 29, RESET);

        assertThat(decision.state()).isEqualTo(state(29, 90_000, 3,
                90_000, 29, 3, 4));
    }

    @Test
    void rejectsUnknownDatabasePolicy() {
        assertThatThrownBy(() -> PlaydataUpsertPolicy.TransitionPolicy.fromDatabase("UNKNOWN"))
                .isInstanceOf(IllegalStateException.class);
    }

    private static PlaydataUpsertPolicy.State state(int currentVersion, int versionScore,
                                                     Integer versionRank, int allTimeScore,
                                                     int allTimeVersion, Integer allTimeRank,
                                                     int medal) {
        return new PlaydataUpsertPolicy.State(currentVersion, versionScore, versionRank,
                allTimeScore, allTimeVersion, allTimeRank, medal);
    }

    private static PlaydataUpsertPolicy.Observation observation(int score, int rank, int medal) {
        return new PlaydataUpsertPolicy.Observation(score, rank, medal);
    }
}
