package gg.popn.domain.game.policy;

import java.util.Arrays;

/**
 * Display and ordering policy for rank codes received from the game.
 *
 * <p>The reference score ranges are intentionally not represented here. A rank
 * is source data and must never be derived from a score by the server.</p>
 */
public enum RankPolicy {
    S_PLUS(1, "S+", 1),
    S(2, "S", 2),
    AAA(3, "AAA", 3),
    AA_PLUS(4, "AA+", 4),
    AA(5, "AA", 5),
    A_PLUS(6, "A+", 6),
    A(7, "A", 7),
    B_PLUS(8, "B+", 8),
    B(9, "B", 9),
    C(10, "C", 10),
    D(11, "D", 11),
    E(12, "E", 12);

    private final int code;
    private final String label;
    private final int sortOrder;

    RankPolicy(int code, String label, int sortOrder) {
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

    public static RankPolicy fromCode(int code) {
        return Arrays.stream(values())
                .filter(policy -> policy.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown rank code: " + code));
    }
}
