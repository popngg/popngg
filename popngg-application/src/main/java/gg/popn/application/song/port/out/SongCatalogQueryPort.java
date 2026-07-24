package gg.popn.application.song.port.out;

import gg.popn.application.song.dto.query.FindSongsQuery;
import gg.popn.application.song.dto.result.GroupedSongView;

import java.util.List;

public interface SongCatalogQueryPort {
    long count(FindSongsQuery query);

    List<GroupedSongView> findPage(FindSongsQuery query);
}
