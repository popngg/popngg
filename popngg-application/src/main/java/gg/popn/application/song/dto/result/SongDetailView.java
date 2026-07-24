package gg.popn.application.song.dto.result;

import java.util.List;

public record SongDetailView(SongMetadataView song, List<ChartMetadataView> charts) {
}
