package gg.popn.infra.db.adapter;

import gg.popn.application.song.dto.command.CreateSongCommand;
import gg.popn.application.song.service.CreateSongService;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.List;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

class ReviewedSongRegistrationConcurrencyTest extends MySqlIntegrationTestSupport {
    @Test void concurrentConfirmationsCreateOneSongAndNoJacket() throws Exception {
        var ds=mysqlDataSource();
        var flyway=Flyway.configure().dataSource(ds).locations("classpath:db/migration").cleanDisabled(false).load();
        flyway.clean(); flyway.migrate();
        var jdbc=new JdbcTemplate(ds);
        jdbc.update("""
                INSERT INTO unknown_chart_reports(report_id,renew_log_id,poptomo_id,song_name,genre_name,
                    artist_name,is_upper,occurrences,resolved,first_seen_at,last_seen_at)
                VALUES(7,1,'test','Song','Genre','Artist',FALSE,1,FALSE,NOW(),NOW())
                """);
        var realCreator=new CreateSongService(new CreateSongJdbcAdapter(new NamedParameterJdbcTemplate(jdbc)));
        var entered=new CountDownLatch(1);
        var release=new CountDownLatch(1);
        var adapter=new ReviewedSongRegistrationJdbcAdapter(jdbc, command -> {
            entered.countDown();
            try { if (!release.await(10,TimeUnit.SECONDS)) throw new IllegalStateException("test timeout"); }
            catch(InterruptedException exception) { Thread.currentThread().interrupt(); throw new IllegalStateException(exception); }
            return realCreator.execute(command);
        });
        var transaction=new TransactionTemplate(new DataSourceTransactionManager(ds));
        transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        var command=new CreateSongCommand(null,"Genre","Song","Artist",29,null,
                List.of(new CreateSongCommand.CreateChartCommand(4,49,29,false,false,false)));
        try(var workers=Executors.newFixedThreadPool(2)) {
            var first=workers.submit(() -> transaction.execute(s -> adapter.register(7,command)));
            assertThat(entered.await(10,TimeUnit.SECONDS)).isTrue();
            var started=new CountDownLatch(1);
            var second=workers.submit(() -> {
                started.countDown();
                try { transaction.execute(s -> adapter.register(7,command)); return "duplicate"; }
                catch(IllegalStateException exception) { return exception.getMessage(); }
            });
            assertThat(started.await(10,TimeUnit.SECONDS)).isTrue();
            release.countDown();
            assertThat(first.get(10,TimeUnit.SECONDS).songId()).isPositive();
            assertThat(second.get(10,TimeUnit.SECONDS)).contains("이미 등록");
        } finally { release.countDown(); }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM songs",Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM charts",Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT jacket_url FROM songs",String.class)).isNull();
        assertThat(jdbc.queryForObject("SELECT resolved FROM unknown_chart_reports WHERE report_id=7",Boolean.class)).isTrue();
    }
}
