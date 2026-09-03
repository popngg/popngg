package gg.popn.infra.db.adapter;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;

/** Derived data and cache revision are changed in the same transaction as their source. */
public final class UserDirectoryState {
    private UserDirectoryState() {}

    // Also serializes summary maintenance with catalog changes/startup reconciliation.
    // Imports must first acquire their user lock, before this or any snapshot SELECT.
    public static void invalidate(JdbcTemplate jdbc) {
        jdbc.update("UPDATE user_directory_revision SET revision = revision + 1 WHERE id = 1");
    }

    public static void refreshUser(JdbcTemplate jdbc, long userId) {
        replace(jdbc, " AND pd.user_id = ?", new Object[]{userId},
                "DELETE FROM user_clear_levels WHERE user_id = ?");
    }

    public static void refreshAll(JdbcTemplate jdbc) {
        replace(jdbc, "", new Object[0], "DELETE FROM user_clear_levels");
    }

    private static void replace(JdbcTemplate jdbc, String filter, Object[] args, String delete) {
        // Read separately: INSERT ... SELECT can take source row locks in MySQL.
        List<Object[]> levels = jdbc.query("""
                SELECT pd.user_id, pd.current_version, MAX(c.level) AS clear_level
                FROM playdata pd JOIN charts c ON c.chart_id = pd.chart_id
                WHERE c.is_deleted = FALSE AND pd.medal_code IN (1,2,3,4,5,6,7,11,12)
                """ + filter + " GROUP BY pd.user_id, pd.current_version",
                (rs, row) -> new Object[]{rs.getLong(1), rs.getInt(2), rs.getInt(3)}, args);
        jdbc.update(delete, args);
        if (!levels.isEmpty()) {
            jdbc.batchUpdate("INSERT INTO user_clear_levels (user_id, current_version, clear_level) VALUES (?, ?, ?)", levels);
        }
    }
}
