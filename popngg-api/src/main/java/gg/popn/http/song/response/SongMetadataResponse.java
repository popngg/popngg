package gg.popn.http.song.response;

import gg.popn.application.song.dto.result.SongMetadataView;

public record SongMetadataResponse(
        long songId,
        String songHash,
        String genreName,
        String songName,
        String artistName,
        int version,
        String jacketUrl
) {
    public static SongMetadataResponse from(SongMetadataView view) {
        return new SongMetadataResponse(view.songId(), view.songHash(), view.genreName(),
                view.songName(), view.artistName(), view.version(), view.jacketUrl());
    }
}
