package gg.popn.application.song.dto.result;

import java.util.List;

public record SongPageView(
        List<GroupedSongView> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static SongPageView of(List<GroupedSongView> content, int page, int size, long totalElements) {
        int totalPages = (int) ((totalElements + size - 1) / size);
        return new SongPageView(content, page, size, totalElements, totalPages);
    }
}
