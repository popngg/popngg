package gg.popn.infra.db.adapter;

import gg.popn.application.playdata.dto.command.ImportPlaydataCommand;
import gg.popn.application.playdata.port.out.UnknownChartReportPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Repository @RequiredArgsConstructor
public class UnknownChartReportJdbcAdapter implements UnknownChartReportPort {
    private final JdbcTemplate jdbc;

    @Override
    public void record(long renewLogId, String poptomoId, List<ImportPlaydataCommand.Row> rows) {
        Instant now = Instant.now();
        for (var row : rows) jdbc.update("""
                INSERT INTO unknown_chart_reports
                    (renew_log_id,poptomo_id,song_name,genre_name,artist_name,difficulty_code,
                     is_upper,occurrences,resolved,first_seen_at,last_seen_at)
                VALUES (?,?,?,?,?,?,?,1,FALSE,?,?)
                ON DUPLICATE KEY UPDATE renew_log_id=VALUES(renew_log_id),
                    poptomo_id=VALUES(poptomo_id), occurrences=occurrences+1,
                    resolved=FALSE,last_seen_at=VALUES(last_seen_at)
                """, renewLogId, poptomoId, row.songName(), row.genreName(),
                row.artistName() == null ? "" : row.artistName(), row.difficultyCode(), row.upper(),
                Timestamp.from(now), Timestamp.from(now));
    }

    @Override
    public List<Report> findRecentUnresolved(int limit) {
        return jdbc.query("""
                SELECT r.report_id,r.song_name,r.genre_name,r.artist_name,r.difficulty_code,r.is_upper,
                       EXISTS (SELECT 1 FROM songs s
                                WHERE s.song_name=r.song_name AND s.genre_name=r.genre_name)
                           AS missing_variant,
                       r.occurrences,r.last_seen_at
                FROM unknown_chart_reports r
                WHERE r.resolved=FALSE
                  AND (NOT EXISTS (
                          SELECT 1 FROM songs s
                           WHERE s.song_name=r.song_name AND s.genre_name=r.genre_name)
                       OR (r.is_upper IS NOT NULL
                           AND NOT EXISTS (
                               SELECT 1 FROM songs s
                               JOIN charts c ON c.song_id=s.song_id AND c.is_deleted=FALSE
                                WHERE s.song_name=r.song_name AND s.genre_name=r.genre_name
                                  AND c.is_upper=r.is_upper)))
                ORDER BY last_seen_at DESC LIMIT ?
                """, (rs, n) -> new Report(rs.getLong("report_id"), rs.getString("song_name"),
                rs.getString("genre_name"), rs.getString("artist_name"),
                (Integer) rs.getObject("difficulty_code"),
                rs.getObject("is_upper") == null ? null : rs.getBoolean("is_upper"),
                rs.getBoolean("missing_variant"), rs.getInt("occurrences"),
                rs.getTimestamp("last_seen_at").toInstant()), limit);
    }

    @Override
    public List<IncompleteReport> findRecentIncomplete(int limit) {
        return jdbc.query("""
                SELECT r.report_id, MIN(s.song_id) AS song_id, r.song_name, r.genre_name,
                       r.artist_name AS reported_artist_name,
                       MIN(COALESCE(s.artist_name,'')) AS registered_artist_name,
                       r.occurrences, r.last_seen_at
                  FROM unknown_chart_reports r
                  JOIN songs s ON s.song_name=r.song_name AND s.genre_name=r.genre_name
                 WHERE r.resolved=FALSE
                 GROUP BY r.report_id,r.song_name,r.genre_name,r.artist_name,
                          r.occurrences,r.last_seen_at
                HAVING COUNT(DISTINCT s.song_id)=1
                   AND MIN(COALESCE(s.artist_name,'')) <> r.artist_name
                 ORDER BY r.last_seen_at DESC LIMIT ?
                """, (rs, n) -> new IncompleteReport(rs.getLong("report_id"), rs.getLong("song_id"),
                rs.getString("song_name"), rs.getString("genre_name"),
                rs.getString("reported_artist_name"), rs.getString("registered_artist_name"),
                rs.getInt("occurrences"), rs.getTimestamp("last_seen_at").toInstant()), limit);
    }

    @Override
    public void resolve(long reportId) {
        jdbc.update("UPDATE unknown_chart_reports SET resolved=TRUE WHERE report_id=?", reportId);
    }
}
