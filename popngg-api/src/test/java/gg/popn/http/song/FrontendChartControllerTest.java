package gg.popn.http.song;

import gg.popn.application.song.dto.result.ChartMetadataView;
import gg.popn.application.song.dto.result.DifficultyView;
import gg.popn.application.song.dto.result.GroupedSongView;
import gg.popn.application.song.dto.result.SongChartView;
import gg.popn.application.song.dto.result.SongDetailView;
import gg.popn.application.song.dto.result.SongMetadataView;
import gg.popn.application.song.dto.result.SongPageView;
import gg.popn.application.song.port.in.FindSongDetailUseCase;
import gg.popn.application.song.port.in.FindSongsUseCase;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FrontendChartControllerTest {
    @Test
    void returnsFrontendListContract() {
        var songs = mock(FindSongsUseCase.class);
        var chart = new SongChartView(2, 48, 4, "EX", 29, true, false, true);
        var song = new GroupedSongView(1, "hash", "genre", "title", "artist", 29,
                "/jacket", List.of(chart));
        when(songs.execute(any())).thenReturn(SongPageView.of(List.of(song), 0, 20, 1));
        var controller = new FrontendChartController(songs, mock(FindSongDetailUseCase.class));

        var response = controller.findCharts(null, null, null, null, null,
                null, null, null, 0, 20).getData().items().getFirst();

        assertThat(response.songId()).isEqualTo(1);
        assertThat(response.isUpper()).isTrue();
        assertThat(response.charts().getFirst().difficulty()).isEqualTo(4);
    }

    @Test
    void flattensSongDetailAndOmitsDeletedCharts() {
        var detail = mock(FindSongDetailUseCase.class);
        var song = new SongMetadataView(1, "hash", "genre", "title", "artist", 29, "/jacket");
        var active = new ChartMetadataView(2, new DifficultyView(4, "EX", "EX", 4),
                48, 29, false, false, true, false);
        var deleted = new ChartMetadataView(3, new DifficultyView(3, "HYPER", "H", 3),
                45, 29, false, false, false, true);
        when(detail.findSong(1)).thenReturn(new SongDetailView(song, List.of(active, deleted)));
        var controller = new FrontendChartController(mock(FindSongsUseCase.class), detail);

        var response = controller.findChart(1).getData();

        assertThat(response.songName()).isEqualTo("title");
        assertThat(response.charts()).extracting(item -> item.chartId()).containsExactly(2L);
    }
}
