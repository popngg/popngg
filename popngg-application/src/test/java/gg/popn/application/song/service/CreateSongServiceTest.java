package gg.popn.application.song.service;

import gg.popn.application.song.dto.command.CreateSongCommand;
import gg.popn.application.song.dto.result.CreateSongResult;
import gg.popn.application.song.port.out.CreateSongPort;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CreateSongServiceTest {
    private final CreateSongPort port = mock(CreateSongPort.class);
    private final CreateSongService service = new CreateSongService(port);

    @Test
    void createsSongWithNormalAndUpperCharts() {
        CreateSongCommand command = command(List.of(chart(4, false), chart(4, true)));
        when(port.create(command)).thenReturn(new CreateSongResult(1, List.of(10L, 11L)));

        assertThat(service.execute(command).chartIds()).containsExactly(10L, 11L);
    }

    @Test
    void rejectsDuplicateDifficultyAndUpperCombination() {
        CreateSongCommand command = command(List.of(chart(4, false), chart(4, false)));

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate");
    }

    @Test
    void rejectsMissingMetadataAndInvalidLevel() {
        assertThatThrownBy(() -> service.execute(
                new CreateSongCommand(null, "", "song", null, 28, null, List.of(chart(1, false)))))
                .isInstanceOf(IllegalArgumentException.class);
        CreateSongCommand invalidLevel = command(List.of(
                new CreateSongCommand.CreateChartCommand(1, 51, 28, false, false, false)));
        assertThatThrownBy(() -> service.execute(invalidLevel))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private CreateSongCommand command(List<CreateSongCommand.CreateChartCommand> charts) {
        return new CreateSongCommand("hash", "genre", "song", "artist", 28, null, charts);
    }

    private CreateSongCommand.CreateChartCommand chart(int difficulty, boolean upper) {
        return new CreateSongCommand.CreateChartCommand(difficulty, 48, 28, upper, false, false);
    }
}
