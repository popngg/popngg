package gg.popn.application.song.port.out;

import gg.popn.application.song.dto.command.CreateSongCommand;
import gg.popn.application.song.dto.result.CreateSongResult;
import java.util.List;

public interface ReviewedSongRegistrationPort {
    List<Long> findExisting(String title, String artist, boolean upper);
    CreateSongResult register(long reportId, CreateSongCommand command);
}
