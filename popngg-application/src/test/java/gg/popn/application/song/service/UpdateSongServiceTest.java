package gg.popn.application.song.service;

import gg.popn.application.song.dto.command.UpdateSongCommand;
import gg.popn.application.song.dto.result.*;
import gg.popn.application.song.port.out.JacketStoragePort;
import gg.popn.application.song.port.out.SongCatalogQueryPort;
import gg.popn.application.song.port.out.UpdateSongPort;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UpdateSongServiceTest {
    private final SongCatalogQueryPort catalog = mock(SongCatalogQueryPort.class);
    private final UpdateSongPort update = mock(UpdateSongPort.class);
    private final JacketStoragePort jackets = mock(JacketStoragePort.class);
    private final UpdateSongService service = new UpdateSongService(catalog, update, jackets);

    @Test
    void updatesMetadataAndCopiesJacketWhenHashChanges() {
        SongDetailView before = detail("old", "old-hash", false);
        SongDetailView after = detail("new", "new-hash", false);
        when(catalog.findSongDetail(1)).thenReturn(Optional.of(before), Optional.of(after));
        when(jackets.copy(eq("old-hash"), anyString())).thenReturn("https://static.popn.gg/new.png");

        assertThat(service.execute(command("new", false))).isEqualTo(after);

        verify(jackets).copy(eq("old-hash"), anyString());
        verify(update).update(argThat(c -> c.jacketUrl().contains("new.png")), anyString());
    }

    @Test
    void removesCopiedObjectWhenDatabaseUpdateFails() {
        when(catalog.findSongDetail(1)).thenReturn(Optional.of(detail("old", "old-hash", false)));
        when(jackets.copy(eq("old-hash"), anyString())).thenReturn("url");
        doThrow(new IllegalStateException("db")).when(update).update(any(), anyString());

        assertThatThrownBy(() -> service.execute(command("new", false)))
                .isInstanceOf(IllegalStateException.class);
        verify(jackets).delete(anyString());
    }

    @Test
    void rejectsMixedUpperAndInvalidLevels() {
        when(catalog.findSongDetail(1)).thenReturn(Optional.of(detail("old", "old-hash", false)));
        var mixed = new UpdateSongCommand(1, "genre", "old", "artist", 29, null, null,
                List.of(new UpdateSongCommand.ChartUpdate(10, 30, 29, false, false, false),
                        new UpdateSongCommand.ChartUpdate(11, 42, 29, true, false, false)));
        assertThatThrownBy(() -> service.execute(mixed)).isInstanceOf(IllegalArgumentException.class);
        var invalid = new UpdateSongCommand(1, "genre", "old", "artist", 29, null, null,
                List.of(new UpdateSongCommand.ChartUpdate(10, 51, 29, false, false, false)));
        assertThatThrownBy(() -> service.execute(invalid)).isInstanceOf(IllegalArgumentException.class);
    }

    private UpdateSongCommand command(String title, boolean upper) {
        return new UpdateSongCommand(1, "genre", title, "artist", 29, null, null,
                List.of(new UpdateSongCommand.ChartUpdate(10, 30, 29, upper, false, false),
                        new UpdateSongCommand.ChartUpdate(11, 42, 29, upper, false, false)));
    }

    private SongDetailView detail(String title, String hash, boolean upper) {
        return new SongDetailView(new SongMetadataView(1, hash, "genre", title, "artist", 29, "old-url"),
                List.of(new ChartMetadataView(10, new DifficultyView(2, "NORMAL", "N", 2),
                                30, 29, upper, false, false, false),
                        new ChartMetadataView(11, new DifficultyView(3, "HYPER", "H", 3),
                                42, 29, upper, false, false, false)));
    }
}
