package gg.popn.application.playdata.service;

import org.junit.jupiter.api.Test;

import static gg.popn.application.playdata.service.PlaydataHistoryPolicy.EventType.*;
import static gg.popn.application.playdata.service.PlaydataUpsertPolicy.TransitionPolicy.CARRY_OVER;
import static gg.popn.application.playdata.service.PlaydataUpsertPolicy.TransitionPolicy.RESET;
import static org.assertj.core.api.Assertions.assertThat;

class PlaydataHistoryPolicyTest {
    private final PlaydataHistoryPolicy policy = new PlaydataHistoryPolicy();

    @Test
    void registersNewState() {
        assertThat(policy.events(null, state(29, 90, 3, 90, 29, 3, 2), null))
                .containsExactly(REGISTER);
    }

    @Test
    void emitsNothingForIdenticalState() {
        var state = state(29, 90, 3, 95, 28, 2, 4);
        assertThat(policy.events(state, state, null)).isEmpty();
    }

    @Test
    void emitsEveryMeaningfulChangeInStableOrder() {
        var previous = state(29, 90, 4, 95, 28, 3, 2);
        var current = state(29, 96, 2, 96, 29, 2, 5);

        assertThat(policy.events(previous, current, null))
                .containsExactly(SCORE_UP, ALL_TIME_SCORE_UP, RANK_CHANGED, MEDAL_CHANGED);
    }

    @Test
    void distinguishesResetAndCarryOverVersionEvents() {
        var previous = state(28, 95, 2, 95, 28, 2, 3);
        var reset = state(29, 80, 4, 95, 28, 2, 3);
        var carried = state(29, 95, 2, 95, 28, 2, 3);

        assertThat(policy.events(previous, reset, RESET)).containsExactly(
                VERSION_INITIALIZED, RANK_CHANGED);
        assertThat(policy.events(previous, carried, CARRY_OVER)).containsExactly(
                VERSION_CARRIED_OVER);
    }

    @Test
    void handlesNullRanks() {
        var previous = state(29, 90, null, 90, 29, null, 2);
        var current = state(29, 90, 2, 90, 29, 2, 2);
        assertThat(policy.events(previous, current, null)).containsExactly(RANK_CHANGED);
    }

    private static PlaydataUpsertPolicy.State state(int version, int versionScore,
                                                     Integer versionRank, int allTimeScore,
                                                     int allTimeVersion, Integer allTimeRank,
                                                     int medal) {
        return new PlaydataUpsertPolicy.State(version, versionScore, versionRank,
                allTimeScore, allTimeVersion, allTimeRank, medal);
    }
}
