package gg.popn.domain.game.policy;

import java.util.Arrays;

/**
 * Medal display policy preserving legacy codes 1-9.
 *
 * <p>Assist easy (assist clear) is assigned code 10 so existing stored codes do not move.
 * Its sort order places it between the weakest normal clear medal and failed
 * medals.</p>
 */
public enum MedalPolicy {
    GOLD_DIAMOND(1, "GOLD DIAMOND", 1),
    SILVER_DIAMOND(2, "SILVER DIAMOND", 2),
    SILVER_STAR(3, "SILVER STAR", 3),
    BRONZE_DIAMOND(4, "BRONZE DIAMOND", 4),
    BRONZE_STAR(5, "BRONZE STAR", 5),
    BRONZE_CIRCLE(6, "BRONZE CIRCLE", 6),
    ASSIST_EASY(10, "ASSIST EASY", 7),
    BLACK_DIAMOND(7, "BLACK DIAMOND", 8),
    BLACK_STAR(8, "BLACK STAR", 9),
    BLACK_CIRCLE(9, "BLACK CIRCLE", 10);

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
