package gg.popn.infra.db.adapter;

import gg.popn.application.tier.AchievementBoard;
import gg.popn.application.tier.AchievementBoardPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public class AchievementBoardJdbcAdapter implements AchievementBoardPort {
    private final JdbcTemplate jdbc;
    public AchievementBoardJdbcAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @Override public List<AchievementBoard.Chart> charts(int level, int version) {
        // Aggregate before joining the catalog so charts with zero achievements remain visible.
        // playdata has a unique (user_id, chart_id) key; each user contributes once.
        return jdbc.query("""
                SELECT c.chart_id, s.song_name, s.genre_name, s.jacket_url,
                       c.difficulty_label, c.is_upper,
                       COALESCE(a.s_count, 0) s_count, COALESCE(a.perfect_count, 0) perfect_count
                FROM charts c JOIN songs s ON s.song_id = c.song_id
                LEFT JOIN (
                    SELECT p.chart_id,
                           SUM(CASE WHEN p.all_time_score BETWEEN 98000 AND 100000 THEN 1 ELSE 0 END) s_count,
                           SUM(CASE WHEN p.medal_code = 1 THEN 1 ELSE 0 END) perfect_count
                    FROM playdata p
                    JOIN charts target ON target.chart_id = p.chart_id
                    JOIN users u ON u.user_id = p.user_id
                    JOIN user_profiles up ON up.user_id = p.user_id
                    WHERE target.level = ? AND target.is_deleted = FALSE AND target.chart_version <= ?
                      AND u.role <> 'BOT' AND up.is_hidden = FALSE
                    GROUP BY p.chart_id
                ) a ON a.chart_id = c.chart_id
                WHERE c.level = ? AND c.is_deleted = FALSE AND c.chart_version <= ?
                ORDER BY c.chart_id
                """, (r, i) -> new AchievementBoard.Chart(r.getLong("chart_id"), r.getString("song_name"),
                r.getString("genre_name"), r.getString("jacket_url"), r.getString("difficulty_label"),
                r.getBoolean("is_upper"), r.getInt("s_count"), r.getInt("perfect_count")),
                level, version, level, version);
    }
}
