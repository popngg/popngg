package gg.popn.application.playdata.service;

import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
public class PopclassPolicy {
    private static final int USER_POPCLASS_DIVISOR = 50;
    public static final int NEW_POPCLASS_SCALE = 1_000;
    private static final long NEW_POPCLASS_DIVISOR = 3_880_000L;

    public int legacyChartPopclass(int level, int score, int medalCode) {
        int medalBonus = medalCode >= 1 && medalCode <= 4
                ? 5_000
                : medalCode >= 5 && medalCode <= 8 ? 3_000 : 0;
        return Math.max(0, (int) Math.floor(
                (level * 10_000L + score - 50_000L + medalBonus) / 54.4));
    }

    public int legacyUserPopclass(Collection<Integer> chartPopclasses) {
        long total = chartPopclasses.stream().mapToLong(Integer::longValue).sum();
        return Math.toIntExact(total / USER_POPCLASS_DIVISOR);
    }

    /**
     * Returns the estimated High Cheers chart value as a thousandths-scaled integer.
     * Medal codes map to PERFECT(1), FULL COMBO(2-4), CLEAR(5-7),
     * EASY CLEAR(8), and LONGOFF CLEAR(12). Failed medals do not receive a
     * clear medal bonus.
     */
    public int newChartPopclass(int level, int score, int medalCode) {
        if (score < 50_000) return 0;
        long value = level * (3_750L * level + newMedalBonus(medalCode)
                + score - 50_000L);
        return Math.toIntExact(value * NEW_POPCLASS_SCALE / NEW_POPCLASS_DIVISOR);
    }

    public int newUserPopclass(Collection<Integer> chartPopclasses) {
        return Math.toIntExact(chartPopclasses.stream().mapToLong(Integer::longValue).sum());
    }

    private static int newMedalBonus(int medalCode) {
        return switch (medalCode) {
            case 1 -> 21_400;
            case 2, 3, 4 -> 17_400;
            case 5, 6, 7 -> 12_400;
            case 8 -> 6_200;
            case 12 -> 9_300;
            default -> 0;
        };
    }

    /** @deprecated use {@link #legacyChartPopclass(int, int, int)}. */
    @Deprecated
    public int chartPopclass(int level, int score, int medalCode) {
        return legacyChartPopclass(level, score, medalCode);
    }

    /** @deprecated use {@link #legacyUserPopclass(Collection)}. */
    @Deprecated
    public int userPopclass(Collection<Integer> chartPopclasses) {
        return legacyUserPopclass(chartPopclasses);
    }
}
