package gg.popn.domain.game.policy;

import java.util.Arrays;

/**
 * Display and ordering policy for the game's clear medals.
 *
 * <p>Codes 1-11 preserve the legacy ordering used by popn.gg. The High Cheers
 * long-pop-off assist clear is appended as code 12 so existing stored values do
 * not move.</p>
 */
public enum MedalPolicy {
    GOLD_STAR(1, "GOLD STAR", 1),
    SILVER_STAR(2, "SILVER STAR", 2),
    SILVER_DIAMOND(3, "SILVER DIAMOND", 3),
    SILVER_CIRCLE(4, "SILVER CIRCLE", 4),
    BRONZE_STAR(5, "BRONZE STAR", 5),
    BRONZE_DIAMOND(6, "BRONZE DIAMOND", 6),
    BRONZE_CIRCLE(7, "BRONZE CIRCLE", 7),
    ASSIST(12, "ASSIST", 8),
    EASY(8, "EASY", 9),
    BLACK_STAR(9, "BLACK STAR", 10),
    BLACK_DIAMOND(10, "BLACK DIAMOND", 11),
    BLACK_CIRCLE(11, "BLACK CIRCLE", 12);

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
