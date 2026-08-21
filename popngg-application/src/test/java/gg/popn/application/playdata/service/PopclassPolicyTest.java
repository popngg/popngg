package gg.popn.application.playdata.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PopclassPolicyTest {
    private final PopclassPolicy policy = new PopclassPolicy();

    @Test
    void calculatesLegacyChartPopclassWithMedalBonus() {
        assertThat(policy.legacyChartPopclass(48, 90_000, 2)).isEqualTo(9_650);
        assertThat(policy.legacyChartPopclass(48, 90_000, 6)).isEqualTo(9_613);
        assertThat(policy.legacyChartPopclass(1, 0, 9)).isZero();
    }

    @Test
    void dividesLegacyUserTotalByFiftyEvenWithFewerRows() {
        assertThat(policy.legacyUserPopclass(List.of(10_000, 9_000))).isEqualTo(380);
    }

    @Test
    void calculatesEstimatedNewChartPopclassInThousandths() {
        assertThat(policy.newChartPopclass(49, 90_000, 4)).isEqualTo(3_045);
        assertThat(policy.newChartPopclass(49, 49_999, 1)).isZero();
    }

    @Test
    void appliesNewMedalBonusesAndSumsSelectedCharts() {
        assertThat(policy.newChartPopclass(50, 92_000, 1))
                .isGreaterThan(policy.newChartPopclass(50, 92_000, 2));
        assertThat(policy.newChartPopclass(50, 92_000, 2))
                .isGreaterThan(policy.newChartPopclass(50, 92_000, 5));
        assertThat(policy.newChartPopclass(50, 92_000, 5))
                .isGreaterThan(policy.newChartPopclass(50, 92_000, 12));
        assertThat(policy.newChartPopclass(50, 92_000, 12))
                .isGreaterThan(policy.newChartPopclass(50, 92_000, 11));
        assertThat(policy.newUserPopclass(List.of(2_982, 3_117))).isEqualTo(6_099);
    }
}
