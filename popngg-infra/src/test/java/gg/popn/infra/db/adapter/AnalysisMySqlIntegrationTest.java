package gg.popn.infra.db.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import gg.popn.infra.analysis.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.*;

class AnalysisMySqlIntegrationTest extends MySqlIntegrationTestSupport {
    @TempDir Path directory;
    @Test void migrationsDeduplicationRetryAndStreamingExtractionWorkOnMysql() throws Exception {
        var ds=mysqlDataSource();
        var flyway=Flyway.configure().dataSource(ds).locations("classpath:db/migration").cleanDisabled(false).load();
        flyway.clean();flyway.migrate();
        var jdbc=new JdbcTemplate(ds);
        jdbc.update("INSERT INTO users(user_id,poptomo_id,password_hash,role,created_at,updated_at) VALUES(1,'0000-0000-0001','test','USER',NOW(),NOW())");
        jdbc.update("INSERT INTO user_profiles(user_id,user_name,created_at,updated_at) VALUES(1,'test',NOW(),NOW())");
        jdbc.update("INSERT INTO songs(song_id,genre_name,song_name,version,extra_type,created_at,updated_at) VALUES(1,'test','test',29,'SUPER_EXTRA',NOW(),NOW())");
        jdbc.update("INSERT INTO charts(chart_id,song_id,difficulty_code,difficulty_label,level,chart_version,has_strict_judgement,has_strict_gauge,created_at,updated_at) VALUES(1,1,3,'EX',49,29,TRUE,TRUE,NOW(),NOW())");
        jdbc.update("INSERT INTO playdata(user_id,chart_id,current_version,all_time_score,medal_code,created_at,updated_at) VALUES(1,1,29,95000,8,NOW(),NOW())");
        var store=new AnalysisJobStore(jdbc,true);
        var tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        var first=tx.execute(s->store.submit("DISCORD","discord:1"));
        var second=tx.execute(s->store.submit("ADMIN","admin:1"));
        assertThat(second.jobId()).isEqualTo(first.jobId());assertThat(second.existing()).isTrue();
        store.started(first.jobId());
        // A new worker can recover this RUNNING row after acquiring the session lock.
        assertThat(store.active().get("job_id")).isEqualTo(first.jobId());
        store.finished(first.jobId(),"SUCCEEDED","{}");
        assertThat(store.notifications()).hasSize(1);
        store.retryNotification(first.jobId());assertThat(store.notifications()).isEmpty();
        assertThat(tx.execute(s->store.submit("ADMIN","admin:1")).jobId()).isEqualTo(first.jobId());
        assertThat(tx.execute(s->store.submit("ADMIN","admin:2")).jobId()).isNotEqualTo(first.jobId());
        var extractor=new AnalysisExtractor(jdbc,new ObjectMapper(),10000);
        tx.setReadOnly(true);tx.setIsolationLevel(org.springframework.transaction.TransactionDefinition.ISOLATION_REPEATABLE_READ);
        var snapshot=tx.execute(s->{try{return extractor.extract(directory);}catch(Exception e){throw new RuntimeException(e);}});
        assertThat(snapshot.metadata().get("extractedRecordCount")).isEqualTo(jdbc.queryForObject("SELECT COUNT(*) FROM playdata",Long.class));
        assertThat(snapshot.catalog()).singleElement().satisfies(chart -> {
            assertThat(chart.strictJudgement()).isTrue();
            assertThat(chart.strictGauge()).isTrue();
            assertThat(chart.extraType()).isEqualTo("SUPER_EXTRA");
        });
        assertThat(directory.resolve("records.jsonl")).exists();
    }
}
