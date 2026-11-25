package gg.popn.application.song.port.in;

import gg.popn.application.song.dto.result.SongPageView;

public interface FindRecentSongsUseCase {
    SongPageView findRecent(int limit);
}
