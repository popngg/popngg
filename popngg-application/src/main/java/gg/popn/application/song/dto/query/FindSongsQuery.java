package gg.popn.application.song.dto.query;

public record FindSongsQuery(
        String keyword,
        Integer version,
        Integer chartVersion,
        Integer level,
        Integer difficulty,
        Boolean isUpper,
        Boolean hasStrictGauge,
        Boolean hasStrictJudgement,
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
    }
}
