package gg.popn.infra.db.adapter;

import gg.popn.application.playdata.dto.command.ImportPlaydataCommand;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UnknownChartReportJdbcAdapterTest {
    @Test
    void recordsAndIncrementsUnknownChart() {
        JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(
                "jdbc:h2:mem:unknown-" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("""
                CREATE TABLE unknown_chart_reports(report_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                renew_log_id BIGINT,poptomo_id VARCHAR(64),song_name VARCHAR(255),genre_name VARCHAR(255),
                artist_name VARCHAR(255),difficulty_code INT,is_upper BOOLEAN,occurrences INT,resolved BOOLEAN,
                first_seen_at TIMESTAMP,last_seen_at TIMESTAMP,
                UNIQUE(song_name,genre_name,artist_name))
                """);
        var adapter = new UnknownChartReportJdbcAdapter(jdbc);
        var row = new ImportPlaydataCommand.Row(null, null, 4, false, null,
                "song", "genre", 1, 1, 1, null, false, null);
        adapter.record(1, "user", List.of(row));
        adapter.record(2, "user", List.of(row));

        var reports = adapter.findRecentUnresolved(10);
        assertThat(reports).hasSize(1);
        assertThat(reports.getFirst().occurrences()).isEqualTo(2);
    }
}
