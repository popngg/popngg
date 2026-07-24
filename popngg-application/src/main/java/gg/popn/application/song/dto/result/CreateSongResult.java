package gg.popn.application.song.dto.result;

import java.util.List;

public record CreateSongResult(long songId, List<Long> chartIds) {
}
