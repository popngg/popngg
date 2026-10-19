package gg.popn.infra.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import gg.popn.application.analysis.AchievementConstantArchive;
import gg.popn.application.analysis.AchievementConstants;
import gg.popn.application.analysis.AnalysisStatistics.Chart;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AchievementRefreshPipelineTest {
    @TempDir Path directory;
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @Test void preparesBothAxesFromTheSameSourceBeforeArchiving() throws Exception {
        var runner=mock(AchievementExperimentRunner.class);
        var constants=mock(AchievementConstants.class);
        var archive=mock(AchievementConstantArchive.class);
        var artifacts=mock(AnalysisArtifacts.class);
        doAnswer(inv -> { writeOutput(inv.getArgument(1), inv.getArgument(2), inv.getArgument(3)); return null; })
                .when(runner).run(any(),any(),anyString(),any());
        when(archive.archive(any())).thenReturn("s3://private/achievement-import.json");
        when(artifacts.upload(anyString(),any(),anyMap(),anyMap())).thenReturn("s3://private/manifest.json");
        var pipeline=new AchievementRefreshPipeline(runner,constants,archive,artifacts,mapper);
        var result=pipeline.prepare(directory,"job/attempt",extracted());
        assertThat(result.medal().sourceSnapshotId()).isEqualTo("job/attempt");
        assertThat(result.rank().sourceSnapshotId()).isEqualTo("job/attempt");
        assertThat(result.summary()).containsKeys("MEDAL","RANK");
        verify(constants,times(2)).validateImport(any());
        verify(archive,times(2)).archive(any());
        verify(artifacts,times(3)).upload(anyString(),any(),anyMap(),anyMap());
    }

    @Test void rejectsMixedSourceBeforeAnyArchiveOrDbImport() throws Exception {
        var runner=mock(AchievementExperimentRunner.class);
        var constants=mock(AchievementConstants.class);
        var archive=mock(AchievementConstantArchive.class);
        var artifacts=mock(AnalysisArtifacts.class);
        doAnswer(inv -> { writeOutput(inv.getArgument(1),"different-source",inv.getArgument(3)); return null; })
                .when(runner).run(any(),any(),anyString(),any());
        var pipeline=new AchievementRefreshPipeline(runner,constants,archive,artifacts,mapper);
        assertThatThrownBy(() -> pipeline.prepare(directory,"job/attempt",extracted()))
                .isInstanceOf(IllegalStateException.class).hasMessage("ACHIEVEMENT_MODEL_NOT_READY");
        verifyNoInteractions(constants,archive,artifacts);
    }

    private AnalysisExtractor.Extracted extracted() {
        return new AnalysisExtractor.Extracted(List.of(new Chart(1,1,49)),List.of(1L),Map.of());
    }

    private void writeOutput(Path output, String sourceId, AchievementConstants.Axis axis) throws Exception {
        Files.createDirectories(output);
        var targets=axis==AchievementConstants.Axis.MEDAL
                ? List.of("CLEAR","BRONZE_DIAMOND","BRONZE_STAR","FULL_COMBO","PERFECT")
                : List.of("AA","AA_PLUS","AAA","S","S_PLUS");
        var rows=new ArrayList<AchievementConstants.ImportRow>();
        for(String target:targets) rows.add(new AchievementConstants.ImportRow(1,"song",49,axis.name(),target,
                1.0,49.0,List.of(48.0,50.0),100,50,"EXPERIMENTAL",List.of()));
        mapper.writeValue(output.resolve("achievement-import.json").toFile(),
                new AchievementConstants.Import(sourceId,"achievement-v1","EXPERIMENTAL",
                        Instant.parse("2026-09-26T00:00:00Z"),axis,rows));
        mapper.writeValue(output.resolve("report.json").toFile(),Map.of(
                "status","EXPERIMENT_COMPLETE","inputKind","DATABASE_SNAPSHOT",
                "sourceSnapshotId",sourceId,"axis",axis.name().toLowerCase(),
                "finalFit",Map.of("converged",true),"connected",true,
                "bootstrapRuns",30,"levelAnchors",List.of(48,49,50)));
    }
}
