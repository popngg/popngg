package gg.popn.application.user.dto.query;

public record FindUsersQuery(
        String keyword, Sort sort, Order order, int page, int size
) {
    public FindUsersQuery {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Invalid users page request.");
        }
    }

    public enum Sort {
        RANK, NAME, CLEAR_LEVEL, UPDATED_AT;

        public static Sort fromApiValue(String value) {
            return switch (value) {
                case "rank" -> RANK;
                case "name" -> NAME;
                case "clearLevel" -> CLEAR_LEVEL;
                case "updatedAt" -> UPDATED_AT;
                default -> throw new IllegalArgumentException("Unknown users sort.");
            };
        }
    }

    public enum Order {
        ASC, DESC;

        public static Order fromApiValue(String value) {
            return switch (value) {
                case "asc" -> ASC;
                case "desc" -> DESC;
                default -> throw new IllegalArgumentException("Unknown users order.");
            };
        }
    }
}
