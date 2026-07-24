package gg.popn.application.playdata.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PopclassPolicyTest {
    private final PopclassPolicy policy = new PopclassPolicy();

    @Test
    void calculatesChartPopclassWithMedalBonus() {
        assertThat(policy.chartPopclass(48, 90_000, 2)).isEqualTo(9_650);
        assertThat(policy.chartPopclass(48, 90_000, 6)).isEqualTo(9_613);
        assertThat(policy.chartPopclass(1, 0, 9)).isZero();
    }

    @Test
    void dividesUserTotalByFiftyEvenWithFewerRows() {
        assertThat(policy.userPopclass(List.of(10_000, 9_000))).isEqualTo(380);
    }
}
