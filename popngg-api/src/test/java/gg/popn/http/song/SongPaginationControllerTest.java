package gg.popn.http.song;

import gg.popn.application.song.dto.result.GroupedSongView;
import gg.popn.application.song.dto.result.SongPageView;
import gg.popn.application.song.port.in.CreateSongUseCase;
import gg.popn.application.song.port.in.FindSongDetailUseCase;
import gg.popn.application.song.port.in.FindSongsUseCase;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SongPaginationControllerTest {
    @Test
    void mapsSongPageToCommonPageResponse() {
        var songs = mock(FindSongsUseCase.class);
        var item = new GroupedSongView(
                1, "hash", "genre", "song", "artist", 29, null, List.of());
        when(songs.execute(any())).thenReturn(SongPageView.of(List.of(item), 1, 20, 41));
        var controller = new SongController(
                songs, mock(FindSongDetailUseCase.class), mock(CreateSongUseCase.class));

        var page = controller.findSongs(
                null, null, null, null, null, null, null, null, 1, 20).getData();

        assertThat(page.items()).singleElement().satisfies(song ->
                assertThat(song.songId()).isEqualTo(1));
        assertThat(page.totalItems()).isEqualTo(41);
        assertThat(page.totalPages()).isEqualTo(3);
        assertThat(page.hasPrev()).isTrue();
        assertThat(page.hasNext()).isTrue();
    }
}
