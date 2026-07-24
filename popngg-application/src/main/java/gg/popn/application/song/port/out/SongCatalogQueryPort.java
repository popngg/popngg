package gg.popn.application.song.port.out;

import gg.popn.application.song.dto.query.FindSongsQuery;
import gg.popn.application.song.dto.result.GroupedSongView;
import gg.popn.application.song.dto.result.ChartDetailView;
import gg.popn.application.song.dto.result.SongDetailView;

import java.util.List;
import java.util.Optional;

public interface SongCatalogQueryPort {
    long count(FindSongsQuery query);

    List<GroupedSongView> findPage(FindSongsQuery query);

    Optional<SongDetailView> findSongDetail(long songId);

    Optional<ChartDetailView> findChartDetail(long chartId);
}
