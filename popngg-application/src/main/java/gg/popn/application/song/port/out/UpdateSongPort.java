package gg.popn.application.song.port.out;

import gg.popn.application.song.dto.command.UpdateSongCommand;

public interface UpdateSongPort {
    void update(UpdateSongCommand command, String songHash);
}
