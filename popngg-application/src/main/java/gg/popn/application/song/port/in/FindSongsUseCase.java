package gg.popn.application.song.port.in;

import gg.popn.application.song.dto.query.FindSongsQuery;
import gg.popn.application.song.dto.result.SongPageView;

public interface FindSongsUseCase {
    SongPageView execute(FindSongsQuery query);
}
