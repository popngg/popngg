package gg.popn.application.song.service;

import gg.popn.application.song.dto.result.ChartDetailView;
import gg.popn.application.song.dto.result.SongDetailView;
import gg.popn.application.song.dto.result.SongMetadataView;
import gg.popn.application.song.exception.CatalogItemNotFoundException;
import gg.popn.application.song.port.out.SongCatalogQueryPort;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FindCatalogDetailServiceTest {
    private final SongCatalogQueryPort port = mock(SongCatalogQueryPort.class);
    private final FindCatalogDetailService service = new FindCatalogDetailService(port);
    private final SongMetadataView song = new SongMetadataView(1, "hash", "genre",
            "song", "artist", 28, null);

    @Test
    void returnsSongAndChartDetails() {
        SongDetailView songDetail = new SongDetailView(song, List.of());
        ChartDetailView chartDetail = new ChartDetailView(song, null);
        when(port.findSongDetail(1)).thenReturn(Optional.of(songDetail));
        when(port.findChartDetail(2)).thenReturn(Optional.of(chartDetail));

        assertThat(service.findSong(1)).isSameAs(songDetail);
        assertThat(service.findChart(2)).isSameAs(chartDetail);
    }

    @Test
    void rejectsUnknownIds() {
        when(port.findSongDetail(9)).thenReturn(Optional.empty());
        when(port.findChartDetail(9)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findSong(9))
                .isInstanceOf(CatalogItemNotFoundException.class);
        assertThatThrownBy(() -> service.findChart(9))
                .isInstanceOf(CatalogItemNotFoundException.class);
    }
}
