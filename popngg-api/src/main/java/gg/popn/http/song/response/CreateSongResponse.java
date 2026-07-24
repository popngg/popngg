package gg.popn.http.song.response;

import gg.popn.application.song.dto.result.CreateSongResult;

import java.util.List;

public record CreateSongResponse(long songId, List<Long> chartIds) {
    public static CreateSongResponse from(CreateSongResult result) {
        return new CreateSongResponse(result.songId(), result.chartIds());
    }
}
