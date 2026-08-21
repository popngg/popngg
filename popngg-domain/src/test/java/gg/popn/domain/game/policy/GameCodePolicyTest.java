package gg.popn.domain.game.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class GameCodePolicyTest {

    @Test
    void highCheersRanksHaveStableCodesLabelsAndOrder() {
        assertThat(RankPolicy.fromCode(1)).isEqualTo(RankPolicy.S_PLUS);
        assertThat(RankPolicy.AA_PLUS.getLabel()).isEqualTo("AA+");
        assertThat(RankPolicy.A_PLUS.getLabel()).isEqualTo("A+");
        assertThat(RankPolicy.B_PLUS.getLabel()).isEqualTo("B+");
        assertUniqueCodesAndSortOrders(
                Arrays.stream(RankPolicy.values()).mapToInt(RankPolicy::getCode).toArray(),
                Arrays.stream(RankPolicy.values()).mapToInt(RankPolicy::getSortOrder).toArray());
    }

    @Test
    void difficultyUsesLightAsTheDisplayLabelForCodeOne() {
        assertThat(DifficultyPolicy.fromCode(1))
                .extracting(
                        DifficultyPolicy::getLabel,
                        DifficultyPolicy::getShortLabel,
                        DifficultyPolicy::getSortOrder)
                .containsExactly("LIGHT", "L", 1);
        assertUniqueCodesAndSortOrders(
                Arrays.stream(DifficultyPolicy.values()).mapToInt(DifficultyPolicy::getCode).toArray(),
                Arrays.stream(DifficultyPolicy.values()).mapToInt(DifficultyPolicy::getSortOrder).toArray());
    }

    @Test
    void definesCompleteClearMedalOrderWithoutRenumberingLegacyCodes() {
        assertThat(MedalPolicy.GOLD_STAR.getCode()).isEqualTo(1);
        assertThat(MedalPolicy.SILVER_CIRCLE.getCode()).isEqualTo(4);
        assertThat(MedalPolicy.BRONZE_CIRCLE.getCode()).isEqualTo(7);
        assertThat(MedalPolicy.EASY_CLEAR.getCode()).isEqualTo(8);
        assertThat(MedalPolicy.EASY_CLEAR.getLabel()).isEqualTo("EASY CLEAR");
        assertThat(MedalPolicy.BLACK_CIRCLE.getCode()).isEqualTo(11);
        assertThat(MedalPolicy.NO_MEDAL.getCode()).isZero();
        assertThat(MedalPolicy.NO_MEDAL.getLabel()).isEqualTo("NONE");
        assertThat(MedalPolicy.LONGOFF_CLEAR.getCode()).isEqualTo(12);
        assertThat(MedalPolicy.LONGOFF_CLEAR.getLabel()).isEqualTo("LONGOFF CLEAR");
        assertThat(MedalPolicy.LONGOFF_CLEAR.getSortOrder())
                .isGreaterThan(MedalPolicy.BRONZE_CIRCLE.getSortOrder())
                .isLessThan(MedalPolicy.EASY_CLEAR.getSortOrder());
        assertThat(MedalPolicy.EASY_CLEAR.getSortOrder())
                .isLessThan(MedalPolicy.BLACK_STAR.getSortOrder());
        assertThat(MedalPolicy.fromCode(10)).isEqualTo(MedalPolicy.BLACK_DIAMOND);
        assertThat(MedalPolicy.fromCode(0)).isEqualTo(MedalPolicy.NO_MEDAL);
        assertThat(MedalPolicy.values()).hasSize(13);
        assertUniqueCodesAndSortOrders(
                Arrays.stream(MedalPolicy.values()).mapToInt(MedalPolicy::getCode).toArray(),
                Arrays.stream(MedalPolicy.values()).mapToInt(MedalPolicy::getSortOrder).toArray());
    }

    @Test
    void unknownCodesAreRejected() {
        assertThatThrownBy(() -> RankPolicy.fromCode(0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MedalPolicy.fromCode(13))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DifficultyPolicy.fromCode(0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static void assertUniqueCodesAndSortOrders(int[] codes, int[] sortOrders) {
        assertThat(codes).doesNotHaveDuplicates();
        assertThat(sortOrders).doesNotHaveDuplicates();
    }
}
