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
    void assistClearDoesNotRenumberLegacyMedalsAndSortsBeforeFailures() {
        assertThat(MedalPolicy.ASSIST_EASY.getCode()).isEqualTo(10);
        assertThat(MedalPolicy.ASSIST_EASY.getLabel()).isEqualTo("ASSIST EASY");
        assertThat(MedalPolicy.ASSIST_EASY.getSortOrder())
                .isGreaterThan(MedalPolicy.BRONZE_CIRCLE.getSortOrder())
                .isLessThan(MedalPolicy.BLACK_DIAMOND.getSortOrder());
        assertThat(MedalPolicy.fromCode(7)).isEqualTo(MedalPolicy.BLACK_DIAMOND);
        assertUniqueCodesAndSortOrders(
                Arrays.stream(MedalPolicy.values()).mapToInt(MedalPolicy::getCode).toArray(),
                Arrays.stream(MedalPolicy.values()).mapToInt(MedalPolicy::getSortOrder).toArray());
    }

    @Test
    void unknownCodesAreRejected() {
        assertThatThrownBy(() -> RankPolicy.fromCode(0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MedalPolicy.fromCode(0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DifficultyPolicy.fromCode(0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static void assertUniqueCodesAndSortOrders(int[] codes, int[] sortOrders) {
        assertThat(codes).doesNotHaveDuplicates();
        assertThat(sortOrders).doesNotHaveDuplicates();
    }
}
