package gg.popn.infra.analysis;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

public class AnalysisJobStoreTest {
    public static Timestamp utc(int precision){return Timestamp.from(Instant.now());}
    @Test void activeAndCompletedRequestsRemainIdempotent() {
        var ds=new DriverManagerDataSource("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1","sa","");
        var jdbc=new JdbcTemplate(ds);
        jdbc.execute("CREATE ALIAS UTC_TIMESTAMP FOR 'gg.popn.infra.analysis.AnalysisJobStoreTest.utc'");
        jdbc.execute("CREATE TABLE analysis_job_guard(guard_id INT PRIMARY KEY)");jdbc.update("INSERT INTO analysis_job_guard VALUES(1)");
        jdbc.execute("CREATE TABLE analysis_jobs(job_id VARCHAR(36) PRIMARY KEY,request_key VARCHAR(120) UNIQUE,trigger_type VARCHAR(16),status VARCHAR(16),active_slot INT UNIQUE,created_at TIMESTAMP,started_at TIMESTAMP,finished_at TIMESTAMP,result_json VARCHAR,notification_pending BOOLEAN DEFAULT FALSE,notification_attempts INT DEFAULT 0,notification_next_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE analysis_job_requests(request_key VARCHAR(120) PRIMARY KEY,job_id VARCHAR(36))");
        var jobs=new AnalysisJobStore(jdbc,true);var tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        var first=tx.execute(s->jobs.submit("DISCORD","discord:1"));
        var alias=tx.execute(s->jobs.submit("ADMIN","admin:1"));
        assertThat(alias.jobId()).isEqualTo(first.jobId());assertThat(alias.existing()).isTrue();
        jobs.started(first.jobId());assertThat(jobs.find(first.jobId()).get("status")).isEqualTo("RUNNING");
        jobs.finished(first.jobId(),"SUCCEEDED","{}");assertThat(jobs.notifications()).hasSize(1);
        assertThat(tx.execute(s->jobs.submit("ADMIN","admin:1")).jobId()).isEqualTo(first.jobId());
        assertThat(tx.execute(s->jobs.submit("DISCORD","discord:1")).jobId()).isEqualTo(first.jobId());
        assertThat(tx.execute(s->jobs.submit("ADMIN","admin:2")).jobId()).isNotEqualTo(first.jobId());
        jobs.notified(first.jobId());assertThat(jobs.notifications()).isEmpty();
        assertThatThrownBy(()->new AnalysisJobStore(jdbc,false).submit("ADMIN","no")).hasMessage("ANALYSIS_DISABLED");
    }
}
