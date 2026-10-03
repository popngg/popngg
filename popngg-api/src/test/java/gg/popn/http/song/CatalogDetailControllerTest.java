package gg.popn.http.song;

import gg.popn.application.song.dto.result.ChartDetailView;
import gg.popn.application.song.dto.result.ChartMetadataView;
import gg.popn.application.song.dto.result.DifficultyView;
import gg.popn.application.song.dto.result.SongDetailView;
import gg.popn.application.song.dto.result.SongMetadataView;
import gg.popn.application.song.port.in.FindChartDetailUseCase;
import gg.popn.application.song.port.in.FindSongDetailUseCase;
import gg.popn.application.song.port.in.FindSongsUseCase;
import gg.popn.application.song.port.in.CreateSongUseCase;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CatalogDetailControllerTest {
    private final SongMetadataView song = new SongMetadataView(
            1, "hash", "High☆Cheers", "Song", "Artist", 28, "/jacket");
    private final ChartMetadataView chart = new ChartMetadataView(
            2, new DifficultyView(1, "LIGHT", "L", 1), 48, 28,
            false, true, true, false);

    @Test
    void mapsSongDetailResponse() {
        FindSongDetailUseCase detailUseCase = mock(FindSongDetailUseCase.class);
        when(detailUseCase.findSong(1)).thenReturn(new SongDetailView(song, List.of(chart)));
        SongController controller = new SongController(
                mock(FindSongsUseCase.class), detailUseCase, mock(CreateSongUseCase.class));

        var response = controller.findSong(1).getData();

        assertThat(response.song().songId()).isEqualTo(1);
        assertThat(response.charts().getFirst().difficulty().label()).isEqualTo("LIGHT");
        assertThat(response.charts().getFirst().hasStrictGauge()).isTrue();
    }

    @Test
    void mapsSeparatedChartDetailResponse() {
        FindChartDetailUseCase useCase = mock(FindChartDetailUseCase.class);
        when(useCase.findChart(2)).thenReturn(new ChartDetailView(song, chart));
        ChartDetailController controller = new ChartDetailController(useCase);

        var response = controller.findChart(2).getData();

        assertThat(response.song().songId()).isEqualTo(1);
        assertThat(response.chart().chartId()).isEqualTo(2);
        assertThat(response.chart().difficulty().shortLabel()).isEqualTo("L");
    }
}
