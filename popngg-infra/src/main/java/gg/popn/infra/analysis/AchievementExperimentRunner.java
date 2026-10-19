package gg.popn.infra.analysis;

import gg.popn.application.analysis.AchievementConstants;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/** Runs the bundled offline model without exposing application credentials to Python. */
@Component
public class AchievementExperimentRunner {
    private static final Set<String> ENVIRONMENT_ALLOWLIST = Set.of(
            "PATH", "SYSTEMROOT", "WINDIR", "TEMP", "TMP", "TMPDIR", "LANG", "LC_ALL");
    private final String executable;
    private final Path script;
    private final int bootstrap;
    private final Duration timeout;
    private final ProcessStarter starter;
    private final Object lifecycle = new Object();
    private Process active;
    private boolean running;
    private boolean closed;

    @Autowired
    public AchievementExperimentRunner(
            @Value("${popngg.achievement.python:python3}") String executable,
            @Value("${popngg.achievement.script:analysis/achievement/experiment.py}") String script,
            @Value("${popngg.achievement.bootstrap:30}") int bootstrap,
            @Value("${popngg.achievement.timeout-seconds:7200}") long timeoutSeconds) {
        this(executable, Path.of(script), bootstrap, Duration.ofSeconds(timeoutSeconds), ProcessBuilder::start);
    }

    AchievementExperimentRunner(String executable, Path script, int bootstrap, Duration timeout,
                                ProcessStarter starter) {
        if (executable == null || executable.isBlank() || bootstrap < 30 || timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("INVALID_ACHIEVEMENT_RUNTIME_CONFIGURATION");
        }
        this.executable = executable;
        this.script = script.toAbsolutePath().normalize();
        this.bootstrap = bootstrap;
        this.timeout = timeout;
        this.starter = starter;
    }

    public void run(Path sourceDirectory, Path outputDirectory, String snapshotId,
                    AchievementConstants.Axis axis) throws Exception {
        synchronized (lifecycle) {
            if (closed) throw new IllegalStateException("ACHIEVEMENT_RUNTIME_CLOSED");
            if (running) throw new IllegalStateException("ACHIEVEMENT_RUNTIME_BUSY");
            running = true;
        }
        Process process = null;
        try {
            Path source = sourceDirectory.toAbsolutePath().normalize();
            Path output = outputDirectory.toAbsolutePath().normalize();
            if (!Files.isRegularFile(script) || !Files.isRegularFile(source.resolve("catalog.json"))
                    || !Files.isRegularFile(source.resolve("records.jsonl"))) {
                throw new IllegalStateException("ACHIEVEMENT_RUNTIME_INPUT_MISSING");
            }
            if (snapshotId == null || snapshotId.isBlank() || axis == null) {
                throw new IllegalArgumentException("INVALID_ACHIEVEMENT_RUNTIME_ARGUMENTS");
            }
            Files.createDirectories(output);
            var builder = new ProcessBuilder(List.of(executable, "-I", "-u", script.toString(),
                    "--snapshot", source.toString(), "--output", output.toString(),
                    "--axis", axis.name().toLowerCase(Locale.ROOT), "--bootstrap", Integer.toString(bootstrap),
                    "--source-snapshot-id", snapshotId));
            builder.directory(output.toFile());
            Map<String, String> environment = restrictedEnvironment(builder.environment());
            builder.environment().clear();
            builder.environment().putAll(environment);
            // A file avoids pipe deadlocks and keeps model output out of API/Discord error messages.
            builder.redirectErrorStream(true);
            builder.redirectOutput(output.resolve("experiment.log").toFile());
            synchronized (lifecycle) {
                if (closed) throw new IllegalStateException("ACHIEVEMENT_RUNTIME_CLOSED");
                try {
                    process = starter.start(builder);
                    active = process;
                } catch (IOException e) {
                    throw new IllegalStateException("ACHIEVEMENT_RUNTIME_UNAVAILABLE");
                }
            }
            if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                terminate(process);
                throw new IllegalStateException("ACHIEVEMENT_EXPERIMENT_TIMEOUT");
            }
            if (process.exitValue() != 0) throw new IllegalStateException("ACHIEVEMENT_EXPERIMENT_FAILED");
        } catch (InterruptedException e) {
            if (process != null) terminate(process);
            Thread.currentThread().interrupt();
            throw e;
        } finally {
            if (process != null && process.isAlive()) terminate(process);
            synchronized (lifecycle) {
                active = null;
                running = false;
            }
        }
    }

    static Map<String, String> restrictedEnvironment(Map<String, String> inherited) {
        var result = new LinkedHashMap<String, String>();
        inherited.forEach((key, value) -> {
            if (ENVIRONMENT_ALLOWLIST.contains(key.toUpperCase(Locale.ROOT))) result.put(key, value);
        });
        // Each job runs two axes sequentially; scientific libraries must not consume all API cores.
        for (String name : List.of("OPENBLAS_NUM_THREADS", "OMP_NUM_THREADS", "MKL_NUM_THREADS",
                "NUMEXPR_NUM_THREADS", "VECLIB_MAXIMUM_THREADS")) result.put(name, "1");
        return result;
    }

    private static void terminate(Process process) {
        process.descendants().forEach(ProcessHandle::destroyForcibly);
        process.destroyForcibly();
        try {
            process.waitFor(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @PreDestroy
    public void shutdown() {
        Process process;
        synchronized (lifecycle) {
            closed = true;
            process = active;
        }
        if (process != null) terminate(process);
    }

    @FunctionalInterface
    interface ProcessStarter {
        Process start(ProcessBuilder builder) throws IOException;
    }
}
