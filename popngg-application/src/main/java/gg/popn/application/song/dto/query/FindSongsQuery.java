package gg.popn.application.song.dto.query;

import java.util.List;

public record FindSongsQuery(
        String keyword,
        Integer version,
        Integer chartVersion,
        Integer levelMin,
        Integer levelMax,
        List<Integer> difficulties,
        Boolean isUpper,
        Boolean hasStrictGauge,
        Boolean hasStrictJudgement,
        Sort sort,
        Order order,
        boolean includeAllCharts,
        int page,
        int size
) {
    public FindSongsQuery {
        if (page < 0) {
            throw new IllegalArgumentException("page must be zero or greater");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }
        keyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        if (keyword != null && keyword.length() > 50) {
            throw new IllegalArgumentException("keyword must not exceed 50 characters");
        }
        if (levelMin != null && (levelMin < 1 || levelMin > 50)
                || levelMax != null && (levelMax < 1 || levelMax > 50)
                || levelMin != null && levelMax != null && levelMin > levelMax) {
            throw new IllegalArgumentException("level range must be between 1 and 50");
        }
        difficulties = difficulties == null || difficulties.isEmpty()
                ? null : List.copyOf(difficulties);
        if (difficulties != null && difficulties.stream()
                .anyMatch(value -> value == null || value < 1 || value > 4)) {
            throw new IllegalArgumentException("difficulty must be between 1 and 4");
        }
        sort = sort == null ? Sort.SONG_ID : sort;
        order = order == null ? Order.ASC : order;
    }

    /** Compatibility constructor for the legacy /songs endpoint. */
    public FindSongsQuery(
            String keyword, Integer version, Integer chartVersion, Integer level,
            Integer difficulty, Boolean isUpper, Boolean hasStrictGauge,
            Boolean hasStrictJudgement, int page, int size) {
        this(keyword, version, chartVersion, level, level,
                difficulty == null ? null : List.of(difficulty),
                isUpper, hasStrictGauge, hasStrictJudgement,
                Sort.SONG_ID, Order.ASC, false, page, size);
    }

    public enum Sort {
        VERSION,
        TITLE,
        GENRE,
        MAX_LEVEL,
        SONG_ID;

        public static Sort from(String value) {
            return switch (value) {
                case "version" -> VERSION;
                case "title" -> TITLE;
                case "genre" -> GENRE;
                case "maxLevel" -> MAX_LEVEL;
                default -> throw new IllegalArgumentException("Unsupported sort: " + value);
            };
        }
    }

    public enum Order {
        ASC,
        DESC;

        public static Order from(String value) {
            return switch (value) {
                case "asc" -> ASC;
                case "desc" -> DESC;
                default -> throw new IllegalArgumentException("Unsupported order: " + value);
            };
        }
    }
}
