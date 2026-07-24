package gg.popn.application.song.port.out;

import gg.popn.application.song.dto.command.CreateSongCommand;
import gg.popn.application.song.dto.result.CreateSongResult;

public interface CreateSongPort {
    CreateSongResult create(CreateSongCommand command);
}
