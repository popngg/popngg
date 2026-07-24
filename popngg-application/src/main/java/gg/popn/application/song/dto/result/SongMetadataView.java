package gg.popn.application.song.dto.result;

public record SongMetadataView(
        long songId,
        String songHash,
        String genreName,
        String songName,
        String artistName,
        int version,
        String jacketUrl
) {
}
