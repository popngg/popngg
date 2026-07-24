package gg.popn.domain.game.policy;

import java.util.Arrays;

public enum DifficultyPolicy {
    LIGHT(1, "LIGHT", "L", 1),
    NORMAL(2, "NORMAL", "N", 2),
    HYPER(3, "HYPER", "H", 3),
    EX(4, "EX", "EX", 4);

    private final int code;
    private final String label;
    private final String shortLabel;
    private final int sortOrder;

    DifficultyPolicy(int code, String label, String shortLabel, int sortOrder) {
        this.code = code;
        this.label = label;
        this.shortLabel = shortLabel;
        this.sortOrder = sortOrder;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public String getShortLabel() {
        return shortLabel;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public static DifficultyPolicy fromCode(int code) {
        return Arrays.stream(values())
                .filter(policy -> policy.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown difficulty code: " + code));
    }
}
