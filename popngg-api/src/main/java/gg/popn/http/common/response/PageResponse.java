package gg.popn.http.common.response;

import java.util.List;

public record PageResponse<T>(
        List<T> items,
        long totalItems,
        int totalPages,
        boolean hasPrev,
        boolean hasNext
) {
    public static <T> PageResponse<T> of(
            List<T> items, long totalItems, int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page must not be negative");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("size must be positive");
        }
        int totalPages = (int) ((totalItems + size - 1) / size);
        return new PageResponse<>(
                List.copyOf(items),
                totalItems,
                totalPages,
                page > 0,
                page + 1 < totalPages);
    }
}
