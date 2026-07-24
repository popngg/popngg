package gg.popn.application.song.service;

import gg.popn.application.song.dto.query.FindSongsQuery;
import gg.popn.application.song.dto.result.GroupedSongView;
import gg.popn.application.song.port.out.SongCatalogQueryPort;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FindSongsServiceTest {
    private final SongCatalogQueryPort port = mock(SongCatalogQueryPort.class);
    private final FindSongsService service = new FindSongsService(port);

    @Test
    void returnsPageMetadataFromQueryPort() {
        FindSongsQuery query = new FindSongsQuery(null, null, null, null, null,
                null, null, null, 1, 2);
        GroupedSongView song = new GroupedSongView(1, "hash", "High☆Cheers",
                "Song", "Artist", 28, null, List.of());
        when(port.count(query)).thenReturn(5L);
        when(port.findPage(query)).thenReturn(List.of(song));

        var result = service.execute(query);

        assertThat(result.content()).containsExactly(song);
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.totalElements()).isEqualTo(5);
        assertThat(result.totalPages()).isEqualTo(3);
    }

    @Test
    void rejectsUnboundedPageSize() {
        assertThatThrownBy(() -> new FindSongsQuery(null, null, null, null, null,
                null, null, null, 0, 101))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
