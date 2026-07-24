package gg.popn.application.playdata.service;

import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
public class PopclassPolicy {
    private static final int USER_POPCLASS_DIVISOR = 50;

    public int chartPopclass(int level, int score, int medalCode) {
        int medalBonus = medalCode >= 1 && medalCode <= 4
                ? 5_000
                : medalCode >= 5 && medalCode <= 8 ? 3_000 : 0;
        return Math.max(0, (int) Math.floor(
                (level * 10_000L + score - 50_000L + medalBonus) / 54.4));
    }

    public int userPopclass(Collection<Integer> chartPopclasses) {
        long total = chartPopclasses.stream().mapToLong(Integer::longValue).sum();
        return Math.toIntExact(total / USER_POPCLASS_DIVISOR);
    }
}
