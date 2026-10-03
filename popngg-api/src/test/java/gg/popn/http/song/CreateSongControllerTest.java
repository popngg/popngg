package gg.popn.http.song;

import gg.popn.application.song.dto.result.CreateSongResult;
import gg.popn.application.song.port.in.CreateSongUseCase;
import gg.popn.application.song.port.in.FindSongDetailUseCase;
import gg.popn.application.song.port.in.FindSongsUseCase;
import gg.popn.http.song.request.CreateSongRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CreateSongControllerTest {
    @Test
    void mapsRequestAndCreatedIds() {
        CreateSongUseCase useCase = mock(CreateSongUseCase.class);
        when(useCase.execute(any())).thenReturn(new CreateSongResult(1, List.of(10L)));
        SongController controller = new SongController(
                mock(FindSongsUseCase.class), mock(FindSongDetailUseCase.class), useCase);
        CreateSongRequest request = new CreateSongRequest("hash", "genre", "song", "artist",
                28, null, List.of(new CreateSongRequest.ChartRequest(
                1, 45, 28, false, true, false)));

        var response = controller.createSong(request).getData();

        assertThat(response.songId()).isEqualTo(1);
        assertThat(response.chartIds()).containsExactly(10L);
    }
}
