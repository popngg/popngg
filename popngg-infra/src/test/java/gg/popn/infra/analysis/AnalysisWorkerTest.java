package gg.popn.infra.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import gg.popn.application.analysis.AnalysisRecord;
import gg.popn.application.analysis.AnalysisStatistics.Chart;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import javax.sql.DataSource;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AnalysisWorkerTest {
    @TempDir Path dir;
    final ObjectMapper mapper=new ObjectMapper();
    @Test void failedUploadDoesNotReplaceLatestAndCompletionFailureRemainsRetryable()throws Exception {
        var jobs=mock(AnalysisJobStore.class);var extractor=mock(AnalysisExtractor.class);var artifacts=mock(AnalysisArtifacts.class);
        var notifier=mock(AnalysisCompletionNotifier.class);var ds=lockedDataSource(true);
        String id=UUID.randomUUID().toString();
        when(jobs.active()).thenReturn(Map.of("job_id",id,"trigger_type","DISCORD"));
        when(extractor.extract(any())).thenAnswer(inv->{Path p=inv.getArgument(0);Files.createDirectories(p);Files.writeString(p.resolve("records.jsonl"),"");return new AnalysisExtractor.Extracted(List.of(),List.of(),Map.of("extractedRecordCount",0L));});
        when(artifacts.upload(anyString(),any(),anyMap(),anyMap())).thenThrow(new IllegalStateException("UPLOAD_FAILED"));
        when(jobs.notifications()).thenReturn(List.of(),List.of(Map.of("job_id",id,"result_json","{\"status\":\"FAILED\"}")));
        doThrow(new IllegalStateException("HTTP_429")).when(notifier).send(anyString());
        var worker=new AnalysisWorker(ds,jobs,extractor,artifacts,notifier,mapper,
                mock(AchievementRefreshPipeline.class),mock(AchievementConstantActivation.class),dir.toString());
        worker.runOnce();worker.shutdown();
        verify(jobs).finished(eq(id),eq("FAILED"),contains("UPLOAD_FAILED"));
        verify(artifacts,never()).publishLatest(anyString(),anyString());
        verify(jobs).retryNotification(id);verify(jobs,never()).notified(id);
    }
    @Test void successfulBuildPublishesBeforePersistingResultAndNotifiesSeparately()throws Exception {
        var jobs=mock(AnalysisJobStore.class);var extractor=mock(AnalysisExtractor.class);var artifacts=mock(AnalysisArtifacts.class);var notifier=mock(AnalysisCompletionNotifier.class);
        String id=UUID.randomUUID().toString();
        when(jobs.active()).thenReturn(Map.of("job_id",id,"trigger_type","SCHEDULED"));
        when(extractor.extract(any())).thenAnswer(inv->{Path p=inv.getArgument(0);Files.createDirectories(p);
            var record=new AnalysisRecord(1,1,1L,49,8,95000,true,true,false,false,true,true,false,false,29,29,95000,true,null,null,1L);
            Files.writeString(p.resolve("records.jsonl"),mapper.writeValueAsString(record)+"\n");
            return new AnalysisExtractor.Extracted(List.of(new Chart(1,1,49)),List.of(1L),Map.of("extractedRecordCount",1L));});
        when(artifacts.upload(anyString(),any(),anyMap(),anyMap())).thenReturn("s3://private/manifest.json");
        var worker=new AnalysisWorker(lockedDataSource(true),jobs,extractor,artifacts,notifier,mapper,
                mock(AchievementRefreshPipeline.class),mock(AchievementConstantActivation.class),dir.toString());
        worker.runOnce();worker.shutdown();
        var order=inOrder(artifacts,jobs);
        order.verify(artifacts).publishLatest(anyString(),eq("s3://private/manifest.json"));
        order.verify(jobs).finished(eq(id),eq("SUCCEEDED"),contains("\"spiRecordCount\":1"));
    }
    @Test void competingReplicaDoesNotBuild()throws Exception {
        var jobs=mock(AnalysisJobStore.class);var extractor=mock(AnalysisExtractor.class);
        var worker=new AnalysisWorker(lockedDataSource(false),jobs,extractor,mock(AnalysisArtifacts.class),mock(AnalysisCompletionNotifier.class),mapper,
                mock(AchievementRefreshPipeline.class),mock(AchievementConstantActivation.class),dir.toString());
        worker.runOnce();worker.shutdown();verifyNoInteractions(jobs,extractor);
    }
    @Test void achievementRefreshActivatesBothAxesAfterPreparationWithoutReplacingCpiLatest() throws Exception {
        var jobs=mock(AnalysisJobStore.class);var extractor=mock(AnalysisExtractor.class);
        var artifacts=mock(AnalysisArtifacts.class);var achievement=mock(AchievementRefreshPipeline.class);
        var activation=mock(AchievementConstantActivation.class);
        String id=UUID.randomUUID().toString();
        when(jobs.active()).thenReturn(Map.of("job_id",id,"trigger_type","DISCORD","job_type","ACHIEVEMENT_CONSTANTS"));
        var extracted=new AnalysisExtractor.Extracted(List.of(),List.of(),Map.of());
        when(extractor.extract(any())).thenReturn(extracted);
        when(achievement.prepare(any(),anyString(),same(extracted)))
                .thenReturn(new AchievementRefreshPipeline.Prepared(null,null,Map.of("MEDAL","s3://private/medal"),Map.of()));
        when(activation.activate(null,null)).thenReturn(List.of());
        var worker=new AnalysisWorker(lockedDataSource(true),jobs,extractor,artifacts,
                mock(AnalysisCompletionNotifier.class),mapper,achievement,activation,dir.toString());
        worker.runOnce();worker.shutdown();
        var order=inOrder(achievement,activation,jobs);
        order.verify(achievement).prepare(any(),anyString(),same(extracted));
        order.verify(activation).activate(null,null);
        order.verify(jobs).finished(eq(id),eq("SUCCEEDED"),contains("ACHIEVEMENT_CONSTANTS"));
        verify(artifacts,never()).publishLatest(anyString(),anyString());
    }
    @Test void achievementPreparationFailureKeepsBothActiveAxesUntouched() throws Exception {
        var jobs=mock(AnalysisJobStore.class);var extractor=mock(AnalysisExtractor.class);
        var achievement=mock(AchievementRefreshPipeline.class);var activation=mock(AchievementConstantActivation.class);
        String id=UUID.randomUUID().toString();
        when(jobs.active()).thenReturn(Map.of("job_id",id,"trigger_type","DISCORD","job_type","ACHIEVEMENT_CONSTANTS"));
        when(extractor.extract(any())).thenReturn(new AnalysisExtractor.Extracted(List.of(),List.of(),Map.of()));
        when(achievement.prepare(any(),anyString(),any())).thenThrow(new IllegalStateException("ACHIEVEMENT_MODEL_NOT_READY"));
        var worker=new AnalysisWorker(lockedDataSource(true),jobs,extractor,mock(AnalysisArtifacts.class),
                mock(AnalysisCompletionNotifier.class),mapper,achievement,activation,dir.toString());
        worker.runOnce();worker.shutdown();
        verifyNoInteractions(activation);
        verify(jobs).finished(eq(id),eq("FAILED"),contains("ACHIEVEMENT_MODEL_NOT_READY"));
    }
    private DataSource lockedDataSource(boolean acquired)throws Exception {
        var ds=mock(DataSource.class);var c=mock(Connection.class);var p=mock(PreparedStatement.class);var r=mock(ResultSet.class);
        when(ds.getConnection()).thenReturn(c);when(c.getCatalog()).thenReturn("test");when(c.prepareStatement(anyString())).thenReturn(p);
        when(p.executeQuery()).thenReturn(r);when(r.next()).thenReturn(true);when(r.getInt(1)).thenReturn(acquired?1:0);when(r.getBoolean(1)).thenReturn(acquired);
        return ds;
    }
}
