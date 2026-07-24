package gg.popn.application.user.dto.query;

public record UserRankingQuery(Sort sort, int page, int size) {
    public UserRankingQuery {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Invalid ranking page request.");
        }
    }

    public enum Sort {
        DISPLAY_POPCLASS,
        POTENTIAL_POPCLASS,
        LEGACY_POPCLASS;

        public static Sort fromApiValue(String value) {
            return switch (value) {
                case "displayPopclass" -> DISPLAY_POPCLASS;
                case "potentialPopclass" -> POTENTIAL_POPCLASS;
                case "legacyPopclass" -> LEGACY_POPCLASS;
                default -> throw new IllegalArgumentException("Unknown ranking sort.");
            };
        }
    }
}
