package gg.popn.http.song;

import gg.popn.application.song.dto.result.GroupedSongView;
import gg.popn.application.song.dto.result.SongPageView;
import gg.popn.application.song.port.in.CreateSongUseCase;
import gg.popn.application.song.port.in.FindSongDetailUseCase;
import gg.popn.application.song.port.in.FindSongsUseCase;
import gg.popn.application.common.ErrorNotificationPort;
import gg.popn.http.common.exception.BaseExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SongPaginationControllerTest {
    @Test
    void recentEndpointDefaultsLimitAndSerializesCreationDate() throws Exception {
        var port = mock(gg.popn.application.song.port.out.SongCatalogQueryPort.class);
        var service = new gg.popn.application.song.service.FindSongsService(port);
        var date = java.time.Instant.parse("2026-10-01T00:00:00Z");
        var song = new GroupedSongView(1, "hash", "genre", "song", "artist", 29, "/banner.png", date, List.of());
        when(port.count(any())).thenReturn(11L);
        when(port.findPage(any())).thenReturn(List.of(song));
        var controller = new SongController(service, mock(FindSongDetailUseCase.class),
                mock(CreateSongUseCase.class));
        var mvc = MockMvcBuilders.standaloneSetup(controller, new RecentChartController(service),
                        new FrontendChartController(service, mock(FindSongDetailUseCase.class)))
                .setControllerAdvice(new BaseExceptionHandler(mock(ErrorNotificationPort.class))).build();
        mvc.perform(get("/api/v1/charts/recent"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.items[0].createdAt").exists())
                .andExpect(jsonPath("$.data.items[0].bannerUrl").value("/banner.png"))
                .andExpect(jsonPath("$.data.items[0].jacketUrl").doesNotExist());
        mvc.perform(get("/api/v1/charts/recent").param("limit", "5"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalPages").value(3));
        mvc.perform(get("/api/v1/songs"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].createdAt").exists());
        for (String invalid : List.of("0", "11", "-1", "abc")) {
            mvc.perform(get("/api/v1/charts/recent").param("limit", invalid)).andExpect(status().isBadRequest());
        }
    }

    @Test
    void mapsSongPageToCommonPageResponse() {
        var songs = mock(FindSongsUseCase.class);
        var item = new GroupedSongView(
                1, "hash", "genre", "song", "artist", 29, null, null, List.of());
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

    @Test
    void rejectsAnOversizedPageAsAClientErrorWithoutIncidentNotification() throws Exception {
        var songs = mock(FindSongsUseCase.class);
        var notifier = mock(ErrorNotificationPort.class);
        var controller = new SongController(
                songs, mock(FindSongDetailUseCase.class), mock(CreateSongUseCase.class));
        var mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new BaseExceptionHandler(notifier))
                .build();

        mvc.perform(get("/api/v1/songs").param("size", "200"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SONG_QUERY"))
                .andExpect(jsonPath("$.message").value("size must be between 1 and 100"));

        verifyNoInteractions(songs, notifier);
    }
}
