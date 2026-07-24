package gg.popn.application.song.port.in;

import gg.popn.application.song.dto.command.CreateSongCommand;
import gg.popn.application.song.dto.result.CreateSongResult;

public interface CreateSongUseCase {
    CreateSongResult execute(CreateSongCommand command);
}
