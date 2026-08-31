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
        jdbc.execute("""
                CREATE TABLE songs(song_id BIGINT PRIMARY KEY, song_name VARCHAR(255),
                genre_name VARCHAR(255), artist_name VARCHAR(255))
                """);
        jdbc.execute("CREATE TABLE charts(chart_id BIGINT PRIMARY KEY, song_id BIGINT, is_upper BOOLEAN, is_deleted BOOLEAN)");
        var adapter = new UnknownChartReportJdbcAdapter(jdbc);
        var row = new ImportPlaydataCommand.Row(null, null, 4, false, null,
                "song", "genre", 1, 1, 1, null, false, null);
        adapter.record(1, "user", List.of(row));
        adapter.record(2, "user", List.of(row));

        var reports = adapter.findRecentUnresolved(10);
        assertThat(reports).hasSize(1);
        assertThat(reports.getFirst().occurrences()).isEqualTo(2);
    }

    @Test
    void separatesIncompleteMetadataFromTrulyUnknownSongsAndResolvesIt() {
        JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(
                "jdbc:h2:mem:incomplete-" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("""
                CREATE TABLE unknown_chart_reports(report_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                renew_log_id BIGINT,poptomo_id VARCHAR(64),song_name VARCHAR(255),genre_name VARCHAR(255),
                artist_name VARCHAR(255),difficulty_code INT,is_upper BOOLEAN,occurrences INT,resolved BOOLEAN,
                first_seen_at TIMESTAMP,last_seen_at TIMESTAMP,
                UNIQUE(song_name,genre_name,artist_name))
                """);
        jdbc.execute("""
                CREATE TABLE songs(song_id BIGINT PRIMARY KEY, song_name VARCHAR(255),
                genre_name VARCHAR(255), artist_name VARCHAR(255))
                """);
        jdbc.execute("CREATE TABLE charts(chart_id BIGINT PRIMARY KEY, song_id BIGINT, is_upper BOOLEAN, is_deleted BOOLEAN)");
        jdbc.update("INSERT INTO songs VALUES (7,'song','genre',NULL)");
        jdbc.update("INSERT INTO charts VALUES (70,7,FALSE,FALSE)");
        var adapter = new UnknownChartReportJdbcAdapter(jdbc);
        var row = new ImportPlaydataCommand.Row(null, null, 4, false, null,
                "song", "genre", 1, 1, 1, null, false, "reported artist");
        adapter.record(1, "user", List.of(row));

        assertThat(adapter.findRecentUnresolved(10)).isEmpty();
        assertThat(adapter.findRecentIncomplete(10)).singleElement().satisfies(report -> {
            assertThat(report.songId()).isEqualTo(7);
            assertThat(report.registeredArtistName()).isEmpty();
            assertThat(report.reportedArtistName()).isEqualTo("reported artist");
        });

        long reportId = adapter.findRecentIncomplete(10).getFirst().reportId();
        adapter.resolve(reportId);
        assertThat(adapter.findRecentIncomplete(10)).isEmpty();

        jdbc.update("UPDATE songs SET artist_name='reported artist' WHERE song_id=7");
        adapter.record(2, "user", List.of(row));
        assertThat(adapter.findRecentIncomplete(10))
                .as("matching metadata must stay out even when a later import reopens the report")
                .isEmpty();
    }

    @Test
    void classifiesMissingUpperVariantSeparatelyFromUnknownSong() {
        JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(
                "jdbc:h2:mem:upper-" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("""
                CREATE TABLE unknown_chart_reports(report_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                renew_log_id BIGINT,poptomo_id VARCHAR(64),song_name VARCHAR(255),genre_name VARCHAR(255),
                artist_name VARCHAR(255),difficulty_code INT,is_upper BOOLEAN,occurrences INT,resolved BOOLEAN,
                first_seen_at TIMESTAMP,last_seen_at TIMESTAMP,
                UNIQUE(song_name,genre_name,artist_name,difficulty_code,is_upper))
                """);
        jdbc.execute("""
                CREATE TABLE songs(song_id BIGINT PRIMARY KEY, song_name VARCHAR(255),
                genre_name VARCHAR(255), artist_name VARCHAR(255))
                """);
        jdbc.execute("CREATE TABLE charts(chart_id BIGINT PRIMARY KEY, song_id BIGINT, is_upper BOOLEAN, is_deleted BOOLEAN)");
        jdbc.update("INSERT INTO songs VALUES (7,'TWINKLING','genre','artist')");
        jdbc.update("INSERT INTO charts VALUES (70,7,FALSE,FALSE)");
        var adapter = new UnknownChartReportJdbcAdapter(jdbc);
        var upper = new ImportPlaydataCommand.Row(null, null, 4, true, null,
                "TWINKLING", "genre", 1, 1, 1, null, false, "artist");

        adapter.record(1, "user", List.of(upper));

        assertThat(adapter.findRecentUnresolved(10)).singleElement().satisfies(report -> {
            assertThat(report.songName()).isEqualTo("TWINKLING");
            assertThat(report.upper()).isTrue();
            assertThat(report.missingVariant()).isTrue();
            assertThat(report.difficultyCode()).isEqualTo(4);
        });
    }
}
