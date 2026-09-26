package gg.popn.infra.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import gg.popn.application.analysis.AnalysisStatistics;
import gg.popn.application.analysis.CpiSpiModel;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import javax.sql.DataSource;
import java.nio.file.*;
import java.sql.Connection;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@EnableScheduling
@ConditionalOnProperty(name="popngg.analysis.enabled",havingValue="true")
public class AnalysisWorker {
    private static final Logger log=LoggerFactory.getLogger(AnalysisWorker.class);
    private final DataSource dataSource; private final AnalysisJobStore jobs; private final AnalysisExtractor extractor;
    private final AnalysisArtifacts artifacts; private final AnalysisCompletionNotifier notifier; private final ObjectMapper mapper;
    private final AchievementRefreshPipeline achievement;
    private final AchievementConstantActivation activation;
    private final Path root; private final ExecutorService executor=Executors.newSingleThreadExecutor();
    private final AtomicBoolean busy=new AtomicBoolean();
    public AnalysisWorker(DataSource dataSource,AnalysisJobStore jobs,AnalysisExtractor extractor,AnalysisArtifacts artifacts,
            AnalysisCompletionNotifier notifier,ObjectMapper mapper, AchievementRefreshPipeline achievement,
            AchievementConstantActivation activation,@Value("${popngg.analysis.output:build/analysis/cpi-spi}") String output) {
        this.dataSource=dataSource;this.jobs=jobs;this.extractor=extractor;this.artifacts=artifacts;this.notifier=notifier;this.mapper=mapper;
        this.achievement=achievement;this.activation=activation;
        this.root=Path.of(output).toAbsolutePath().normalize();
    }
    @Scheduled(cron="${popngg.analysis.cron:0 0 6 * * *}",zone="Asia/Seoul")
    public void daily() {
        try {jobs.submit("SCHEDULED","daily:"+LocalDate.now(ZoneId.of("Asia/Seoul")));}
        catch(RuntimeException e){log.error("Could not enqueue daily analysis ({})",e.getClass().getSimpleName());}
    }
    @Scheduled(fixedDelayString="${popngg.analysis.poll-ms:5000}",initialDelayString="${popngg.analysis.poll-ms:5000}")
    public void poll() {
        if(!busy.compareAndSet(false,true))return;
        try {executor.submit(()->{try {runOnce();} finally {busy.set(false);}});}
        catch(RejectedExecutionException e){busy.set(false);}
    }
    /** Session-scoped lock prevents concurrent builds across API replicas and releases on process death. */
    void runOnce() {
        try(Connection connection=dataSource.getConnection()) {
            String lock="popngg-analysis-"+connection.getCatalog();
            try(var p=connection.prepareStatement("SELECT GET_LOCK(?,0)")) {
                p.setString(1,lock);try(var r=p.executeQuery()){if(!r.next() || r.getInt(1)!=1)return;}
            }
            try {
                deliverNotifications();
                var job=jobs.active(); if(!job.isEmpty()) build(connection,lock,job);
                deliverNotifications();
            } finally {
                try(var p=connection.prepareStatement("SELECT RELEASE_LOCK(?)")){p.setString(1,lock);p.execute();}
            }
        }catch(Exception e){log.error("Analysis worker failed ({})",e.getClass().getSimpleName());}
    }
    private void build(Connection connection,String lock,Map<String,Object> job) throws Exception {
        String id=(String)job.get("job_id"); UUID.fromString(id);
        jobs.started(id);
        // A recovered job uses a new attempt directory; partially uploaded attempts are never latest.
        String snapshotId=id+"/"+UUID.randomUUID();
        Path directory=root.resolve(snapshotId).normalize();
        if(!directory.startsWith(root))throw new IllegalStateException("INVALID_OUTPUT_PATH");
        var result=new LinkedHashMap<String,Object>();
        result.put("jobId",id);result.put("trigger",job.get("trigger_type"));result.put("snapshotId",snapshotId);
        String jobType = (String)job.getOrDefault("job_type", "CPI_SPI");
        result.put("jobType", jobType);
        try {
            artifacts.checkPrivateBucket();
            var extracted=extractor.extract(directory);
            if ("ACHIEVEMENT_CONSTANTS".equals(jobType)) {
                var prepared = achievement.prepare(directory, snapshotId, extracted);
                assertLock(connection, lock);
                result.put("stored", activation.activate(prepared.medal(), prepared.rank()));
                result.put("status", "SUCCEEDED");
                result.put("artifacts", prepared.artifacts());
                result.put("summary", prepared.summary());
                result.put("modelStatus", "EXPERIMENTAL");
                result.put("nextCommand", "/상수표");
            } else {
            var summary=new AnalysisStatistics(mapper).analyze(directory,extracted.catalog(),extracted.users());
            summary.putAll(new CpiSpiModel(mapper).fit(directory,extracted.catalog()));
            mapper.writerWithDefaultPrettyPrinter().writeValue(directory.resolve("summary.json").toFile(),summary);
            if(!Objects.equals(((Number)summary.get("rawRecordCount")).longValue(),((Number)extracted.metadata().get("extractedRecordCount")).longValue()))
                throw new IllegalStateException("ANALYSIS_COUNT_MISMATCH");
            String manifest=artifacts.upload(snapshotId,directory,extracted.metadata(),summary);
            assertLock(connection,lock);
            artifacts.publishLatest(snapshotId,manifest);
            result.put("status","SUCCEEDED");result.put("artifacts",Map.of("manifest",manifest,"report",manifest.replace("manifest.json","report.md")));
            result.put("summary",summary);
            }
        }catch(Exception e) {
            result.put("status","FAILED");result.put("errorCode",e instanceof IllegalStateException && e.getMessage()!=null && e.getMessage().matches("[A-Z_]+")?e.getMessage():"ANALYSIS_FAILED");
            // Never include DB URLs, record values, or HTTP credentials in Discord errors.
            log.error("Analysis job {} failed ({})",id,e.getClass().getSimpleName());
        }
        assertLock(connection,lock);
        result.put("finishedAt",Instant.now().toString());
        jobs.finished(id,(String)result.get("status"),mapper.writeValueAsString(result));
    }
    private void assertLock(Connection connection,String lock)throws Exception {
        try(var p=connection.prepareStatement("SELECT IS_USED_LOCK(?) = CONNECTION_ID()")) {
            p.setString(1,lock);try(var r=p.executeQuery()){if(!r.next() || !r.getBoolean(1))throw new IllegalStateException("ANALYSIS_LOCK_LOST");}
        }
    }
    private void deliverNotifications() {
        for(var row:jobs.notifications()) {
            String id=(String)row.get("job_id");
            try {notifier.send((String)row.get("result_json"));jobs.notified(id);}
            catch(Exception e){jobs.retryNotification(id);log.warn("Analysis completion notification queued for retry: {}",id);}
        }
    }
    @PreDestroy public void shutdown(){executor.shutdownNow();}
}
