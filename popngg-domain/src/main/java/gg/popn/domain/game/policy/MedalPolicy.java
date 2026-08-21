package gg.popn.domain.game.policy;

import java.util.Arrays;

/**
 * Display and ordering policy for the game's clear medals.
 *
 * <p>Codes 1-12 follow the High Cheers source order: regular medals A-J,
 * easy clear K, and long-pop-off clear L. Code 0 represents source data that
 * reports no medal and uses code 13.</p>
 */
public enum MedalPolicy {
    GOLD_STAR(1, "GOLD STAR", 1),
    SILVER_STAR(2, "SILVER STAR", 2),
    SILVER_DIAMOND(3, "SILVER DIAMOND", 3),
    SILVER_CIRCLE(4, "SILVER CIRCLE", 4),
    BRONZE_STAR(5, "BRONZE STAR", 5),
    BRONZE_DIAMOND(6, "BRONZE DIAMOND", 6),
    BRONZE_CIRCLE(7, "BRONZE CIRCLE", 7),
    BLACK_STAR(8, "BLACK STAR", 8),
    BLACK_DIAMOND(9, "BLACK DIAMOND", 9),
    BLACK_CIRCLE(10, "BLACK CIRCLE", 10),
    EASY_CLEAR(11, "EASY CLEAR", 11),
    LONGOFF_CLEAR(12, "LONGOFF CLEAR", 12),
    NO_MEDAL(13, "NONE", 13);

    private final int code;
    private final String label;
    private final int sortOrder;

    MedalPolicy(int code, String label, int sortOrder) {
        this.code = code;
        this.label = label;
        this.sortOrder = sortOrder;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public static MedalPolicy fromCode(int code) {
        return Arrays.stream(values())
                .filter(policy -> policy.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown medal code: " + code));
    }
}
