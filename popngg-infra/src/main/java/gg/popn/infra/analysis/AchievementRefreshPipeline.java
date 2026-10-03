package gg.popn.infra.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gg.popn.application.analysis.AchievementConstantArchive;
import gg.popn.application.analysis.AchievementConstants;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Prepares both axes without modifying the active DB snapshots or CPI/SPI latest.json. */
@Component
public class AchievementRefreshPipeline {
    private final AchievementExperimentRunner runner;
    private final AchievementConstants constants;
    private final AchievementConstantArchive archive;
    private final AnalysisArtifacts artifacts;
    private final ObjectMapper mapper;

    public AchievementRefreshPipeline(AchievementExperimentRunner runner, AchievementConstants constants,
            AchievementConstantArchive archive, AnalysisArtifacts artifacts, ObjectMapper mapper) {
        this.runner=runner; this.constants=constants; this.archive=archive; this.artifacts=artifacts; this.mapper=mapper;
    }

    public record Prepared(AchievementConstants.Import medal, AchievementConstants.Import rank,
                           Map<String,Object> artifacts, Map<String,Object> summary) {}

    public Prepared prepare(Path source, String snapshotId, AnalysisExtractor.Extracted extracted) throws Exception {
        var outputs = new EnumMap<AchievementConstants.Axis, AchievementConstants.Import>(AchievementConstants.Axis.class);
        var summary = new LinkedHashMap<String,Object>();
        var reports = new EnumMap<AchievementConstants.Axis, Map<String,Object>>(AchievementConstants.Axis.class);
        Set<Long> expectedCharts = new HashSet<>();
        extracted.catalog().stream().filter(c -> c.level() >= 48 && c.level() <= 50)
                .forEach(c -> expectedCharts.add(c.chartId()));
        if (expectedCharts.isEmpty()) throw new IllegalStateException("ACHIEVEMENT_CATALOG_EMPTY");
        for (var axis : AchievementConstants.Axis.values()) {
            Path output = source.resolve(axis.name().toLowerCase(Locale.ROOT));
            runner.run(source, output, snapshotId, axis);
            JsonNode report = mapper.readTree(output.resolve("report.json").toFile());
            if (!"EXPERIMENT_COMPLETE".equals(report.path("status").asText())
                    || !"DATABASE_SNAPSHOT".equals(report.path("inputKind").asText())
                    || !axis.name().equalsIgnoreCase(report.path("axis").asText())
                    || !snapshotId.equals(report.path("sourceSnapshotId").asText())
                    || !report.path("finalFit").path("converged").asBoolean()
                    || !report.path("connected").asBoolean()
                    || report.path("bootstrapRuns").asInt() < 30
                    || report.path("levelAnchors").size() != 3)
                throw new IllegalStateException("ACHIEVEMENT_MODEL_NOT_READY");
            Path bundlePath = output.resolve("achievement-import.json");
            if (!Files.isRegularFile(bundlePath) || Files.size(bundlePath) > 10_000_000)
                throw new IllegalStateException("ACHIEVEMENT_OUTPUT_INVALID");
            var bundle = mapper.readValue(bundlePath.toFile(), AchievementConstants.Import.class);
            validateOutput(bundle, snapshotId, axis, expectedCharts);
            constants.validateImport(bundle);
            outputs.put(axis, bundle);
            long ready = bundle.constants().stream().filter(c -> "EXPERIMENTAL".equals(c.status())).count();
            if (ready == 0) throw new IllegalStateException("ACHIEVEMENT_NO_CALCULATED_CONSTANTS");
            var axisSummary = new LinkedHashMap<String,Object>();
            axisSummary.put("rowCount", bundle.constants().size());
            axisSummary.put("chartCount", expectedCharts.size());
            axisSummary.put("calculatedCount", ready);
            axisSummary.put("holdCount", bundle.constants().size() - ready);
            axisSummary.put("userCount", report.path("users").asInt());
            axisSummary.put("recordCount", report.path("quality").path("valid").asLong());
            axisSummary.put("bootstrapRuns", report.path("bootstrapRuns").asInt());
            axisSummary.put("bootstrapConverged", report.path("bootstrapConverged").asInt());
            summary.put(axis.name(), axisSummary);
            reports.put(axis, axisSummary);
        }

        // All model outputs are checked before any DB activation. S3 objects are immutable attempt artifacts.
        var saved = new LinkedHashMap<String,Object>();
        for (var axis : AchievementConstants.Axis.values()) {
            String name = axis.name().toLowerCase(Locale.ROOT);
            String json = archive.archive(outputs.get(axis));
            String manifest = artifacts.upload(snapshotId + "/" + name, source.resolve(name),
                    extracted.metadata(), reports.get(axis));
            saved.put(axis.name(), Map.of("json", json, "manifest", manifest,
                    "report", manifest.replace("manifest.json", "report.json")));
        }
        saved.put("sourceManifest", artifacts.upload(snapshotId, source, extracted.metadata(), summary));
        return new Prepared(outputs.get(AchievementConstants.Axis.MEDAL), outputs.get(AchievementConstants.Axis.RANK),
                Map.copyOf(saved), Map.copyOf(summary));
    }

    private static void validateOutput(AchievementConstants.Import bundle, String snapshotId,
            AchievementConstants.Axis axis, Set<Long> expectedCharts) throws IOException {
        if (bundle == null || bundle.axis() != axis || !snapshotId.equals(bundle.sourceSnapshotId())
                || bundle.constants() == null || bundle.constants().size() != expectedCharts.size() * 5)
            throw new IllegalStateException("ACHIEVEMENT_OUTPUT_INVALID");
        Set<String> targets = axis == AchievementConstants.Axis.MEDAL
                ? Set.of("CLEAR", "BRONZE_DIAMOND", "BRONZE_STAR", "FULL_COMBO", "PERFECT")
                : Set.of("AA", "AA_PLUS", "AAA", "S", "S_PLUS");
        Set<String> keys = new HashSet<>();
        for (var row : bundle.constants()) {
            if (!expectedCharts.contains(row.chartId()) || !targets.contains(row.target())
                    || !keys.add(row.chartId() + ":" + row.target()))
                throw new IllegalStateException("ACHIEVEMENT_OUTPUT_INVALID");
        }
    }
}
