package gg.popn.application.song.port.in;

import gg.popn.application.song.dto.result.SongDetailView;

public interface FindSongDetailUseCase {
    SongDetailView findSong(long songId);

    SongDetailView findSong(String songHash);
}
