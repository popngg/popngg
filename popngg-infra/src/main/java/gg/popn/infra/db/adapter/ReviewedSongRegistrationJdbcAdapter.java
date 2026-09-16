package gg.popn.infra.db.adapter;

import gg.popn.application.song.dto.command.CreateSongCommand;
import gg.popn.application.song.dto.result.CreateSongResult;
import gg.popn.application.song.port.in.CreateSongUseCase;
import gg.popn.application.song.port.out.ReviewedSongRegistrationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository @RequiredArgsConstructor
public class ReviewedSongRegistrationJdbcAdapter implements ReviewedSongRegistrationPort {
    private final JdbcTemplate jdbc;
    private final CreateSongUseCase createSong;

    @Override
    public List<Long> findExisting(String title, String artist, boolean upper) {
        // Ignore genre and debut version: these may be stale. Include deleted charts and chartless songs.
        return jdbc.query("""
                SELECT s.song_id FROM songs s
                 WHERE s.song_name=? AND s.artist_name=?
                   AND (NOT EXISTS (SELECT 1 FROM charts c WHERE c.song_id=s.song_id)
                        OR EXISTS (SELECT 1 FROM charts c WHERE c.song_id=s.song_id AND c.is_upper=?))
                """, (rs, n) -> rs.getLong(1), title, artist, upper);
    }

    @Override @Transactional(isolation = Isolation.READ_COMMITTED)
    public CreateSongResult register(long reportId, CreateSongCommand command) {
        if (command.jacketUrl() != null || command.charts() == null || command.charts().isEmpty())
            throw new IllegalArgumentException("Official registration requires charts and no jacket");
        // The same lock is taken by all catalog writers. Recheck after waiting for a competing registration.
        UserDirectoryState.invalidate(jdbc);
        var existing = findExisting(command.songName(), command.artistName(), command.charts().getFirst().isUpper());
        if (!existing.isEmpty()) throw new IllegalStateException("이미 등록된 곡입니다: " + existing + ". `/곡수정`을 이용해 주세요.");
        var reports = jdbc.query("SELECT song_name, artist_name, resolved FROM unknown_chart_reports WHERE report_id=?",
                (rs, n) -> !rs.getBoolean("resolved") && rs.getString("song_name").equals(command.songName())
                        && rs.getString("artist_name").equals(command.artistName()), reportId);
        if (reports.size() != 1 || !reports.getFirst()) throw new IllegalStateException("미등록 항목이 변경되었거나 처리되었습니다.");
        CreateSongResult result = createSong.execute(command);
        jdbc.update("UPDATE unknown_chart_reports SET resolved=TRUE WHERE report_id=?", reportId);
        return result;
    }
}
