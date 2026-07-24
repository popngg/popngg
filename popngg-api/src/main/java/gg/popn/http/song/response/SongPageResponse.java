package gg.popn.http.song.response;

import gg.popn.application.song.dto.result.SongPageView;

import java.util.List;

public record SongPageResponse(
        List<GroupedSongResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static SongPageResponse from(SongPageView view) {
        return new SongPageResponse(view.content().stream().map(GroupedSongResponse::from).toList(),
                view.page(), view.size(), view.totalElements(), view.totalPages());
    }
}
