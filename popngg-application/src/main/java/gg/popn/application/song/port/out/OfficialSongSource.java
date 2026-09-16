package gg.popn.application.song.port.out;

import java.util.List;

/** Public metadata only. The official list does not supply release dates or strict flags. */
public interface OfficialSongSource {
    List<Song> find(String title, String artist, Boolean upper);

    record Song(String title, String genre, String artist, int version, boolean upper,
                List<Chart> charts, String sourceUrl) {}
    record Chart(int difficulty, int level) {}
}
