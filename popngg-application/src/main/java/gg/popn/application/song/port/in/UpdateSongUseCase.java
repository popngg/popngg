package gg.popn.application.song.port.in;

import gg.popn.application.song.dto.command.UpdateSongCommand;
import gg.popn.application.song.dto.result.SongDetailView;

public interface UpdateSongUseCase {
    SongDetailView execute(UpdateSongCommand command);
}
