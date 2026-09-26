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
    private record Fixture(JdbcTemplate jdbc,AnalysisJobStore jobs,TransactionTemplate tx) {}
    private Fixture fixture() {
        var ds=new DriverManagerDataSource("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1","sa","");
        var jdbc=new JdbcTemplate(ds);
        jdbc.execute("CREATE ALIAS UTC_TIMESTAMP FOR 'gg.popn.infra.analysis.AnalysisJobStoreTest.utc'");
        jdbc.execute("CREATE TABLE analysis_job_guard(guard_id INT PRIMARY KEY)");jdbc.update("INSERT INTO analysis_job_guard VALUES(1)");
        jdbc.execute("CREATE TABLE analysis_jobs(job_id VARCHAR(36) PRIMARY KEY,request_key VARCHAR(120) UNIQUE,trigger_type VARCHAR(16),job_type VARCHAR(32) NOT NULL DEFAULT 'CPI_SPI',status VARCHAR(16),active_slot INT UNIQUE,created_at TIMESTAMP,started_at TIMESTAMP,finished_at TIMESTAMP,result_json VARCHAR,notification_pending BOOLEAN DEFAULT FALSE,notification_attempts INT DEFAULT 0,notification_next_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE analysis_job_requests(request_key VARCHAR(120) PRIMARY KEY,job_id VARCHAR(36))");
        return new Fixture(jdbc,new AnalysisJobStore(jdbc,true),new TransactionTemplate(new DataSourceTransactionManager(ds)));
    }
    @Test void activeAndCompletedRequestsRemainIdempotent() {
        var f=fixture();var jdbc=f.jdbc();var jobs=f.jobs();var tx=f.tx();
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

    @Test void differentJobTypesQueueSeparatelyAndExecuteOldestFirst() {
        var f=fixture();var jobs=f.jobs();var tx=f.tx();
        var cpi=tx.execute(s->jobs.submit("DISCORD","discord:1"));
        f.jdbc().update("UPDATE analysis_jobs SET created_at=? WHERE job_id=?",Timestamp.from(Instant.parse("2026-01-01T00:00:00Z")),cpi.jobId());
        jobs.started(cpi.jobId());
        var constants=tx.execute(s->jobs.submitAchievements("DISCORD","discord-achievements:2"));
        assertThat(constants.jobId()).isNotEqualTo(cpi.jobId());
        assertThat(constants.existing()).isFalse();
        assertThat(jobs.find(constants.jobId())).containsEntry("status","QUEUED").containsEntry("job_type","ACHIEVEMENT_CONSTANTS");
        assertThat(jobs.active()).containsEntry("job_id",cpi.jobId()).containsEntry("job_type","CPI_SPI");
        var alias=tx.execute(s->jobs.submitAchievements("ADMIN","admin:constants"));
        assertThat(alias.jobId()).isEqualTo(constants.jobId());assertThat(alias.existing()).isTrue();
        jobs.finished(cpi.jobId(),"SUCCEEDED","{}");
        assertThat(jobs.active()).containsEntry("job_id",constants.jobId()).containsEntry("job_type","ACHIEVEMENT_CONSTANTS");
        jobs.started(constants.jobId());
        assertThat(jobs.find(constants.jobId())).containsEntry("status","RUNNING");
        jobs.finished(constants.jobId(),"SUCCEEDED","{\"jobType\":\"ACHIEVEMENT_CONSTANTS\"}");
        assertThat(jobs.active()).isEmpty();assertThat(jobs.notifications()).hasSize(2);
        assertThat(tx.execute(s->jobs.submitAchievements("DISCORD","discord-achievements:2")).jobId()).isEqualTo(constants.jobId());
        assertThat(tx.execute(s->jobs.submitAchievements("ADMIN","admin:constants")).jobId()).isEqualTo(constants.jobId());
        assertThat(tx.execute(s->jobs.submitAchievements("ADMIN","admin:new")).jobId()).isNotEqualTo(constants.jobId());
    }

    @Test void requestKeyCannotResolveToDifferentJobType() {
        var f=fixture();
        f.tx().execute(s->f.jobs().submit("ADMIN","shared"));
        assertThatThrownBy(()->f.tx().execute(s->f.jobs().submitAchievements("ADMIN","shared")))
                .hasMessage("ANALYSIS_REQUEST_TYPE_MISMATCH");
        assertThat(f.jdbc().queryForObject("SELECT COUNT(*) FROM analysis_jobs",Integer.class)).isEqualTo(1);
        assertThatThrownBy(()->f.tx().execute(s->f.jobs().submitAchievements("ADMIN"," ")))
                .hasMessage("INVALID_ANALYSIS_REQUEST");
        assertThatThrownBy(()->new AnalysisJobStore(f.jdbc(),false).submitAchievements("ADMIN","no"))
                .hasMessage("ANALYSIS_DISABLED");
    }
}
