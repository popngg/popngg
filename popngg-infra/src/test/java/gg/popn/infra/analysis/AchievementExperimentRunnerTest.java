package gg.popn.infra.analysis;

import gg.popn.application.analysis.AchievementConstants.Axis;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AchievementExperimentRunnerTest {
    @TempDir Path directory;

    @Test void preservesArgumentsAsLiteralsAndUsesIsolatedPythonWithLocalLog() throws Exception {
        var process = finishedProcess(0);
        var captured = new AtomicReference<ProcessBuilder>();
        var runner = runner(builder -> { captured.set(builder); return process; });
        Path source = source();
        Path output = directory.resolve("output with spaces");
        runner.run(source, output, "snapshot/name ; literal", Axis.RANK);
        var builder = captured.get();
        assertThat(builder.command()).containsExactly("python3", "-I", "-u", script().toString(),
                "--snapshot", source.toString(), "--output", output.toString(), "--axis", "rank",
                "--bootstrap", "30", "--source-snapshot-id", "snapshot/name ; literal");
        assertThat(builder.directory()).isEqualTo(output.toFile());
        assertThat(builder.redirectErrorStream()).isTrue();
        assertThat(builder.redirectOutput().file()).isEqualTo(output.resolve("experiment.log").toFile());
        assertThat(builder.environment()).doesNotContainKeys("AWS_ACCESS_KEY_ID", "AWS_SECRET_ACCESS_KEY",
                "JWT_SECRET_KEY", "DISCORD_BOT_TOKEN", "SPRING_DATASOURCE_PASSWORD", "PYTHONPATH");
        assertThat(builder.environment()).containsEntry("OPENBLAS_NUM_THREADS", "1")
                .containsEntry("OMP_NUM_THREADS", "1");
        runner.shutdown();
    }

    @Test void environmentOnlyKeepsOsRuntimeValuesAndCapsNativeThreads() {
        assertThat(AchievementExperimentRunner.restrictedEnvironment(Map.of(
                "Path", "runtime", "SYSTEMROOT", "system", "TMP", "temporary", "LANG", "C.UTF-8",
                "AWS_SECRET_ACCESS_KEY", "secret", "PYTHONPATH", "unsafe", "OPENBLAS_NUM_THREADS", "64")))
                .containsEntry("Path", "runtime").containsEntry("SYSTEMROOT", "system")
                .containsEntry("TMP", "temporary").containsEntry("LANG", "C.UTF-8")
                .containsEntry("OPENBLAS_NUM_THREADS", "1")
                .doesNotContainKeys("AWS_SECRET_ACCESS_KEY", "PYTHONPATH");
    }

    @Test void timeoutTerminatesProcessTreeAndAllowsLaterAttempt() throws Exception {
        var process = finishedProcess(0);
        var child = mock(ProcessHandle.class);
        when(process.waitFor(anyLong(), eq(TimeUnit.MILLISECONDS))).thenReturn(false, true);
        when(process.descendants()).thenAnswer(inv -> Stream.of(child));
        var runner = runner(builder -> process);
        Path source = source();
        assertThatThrownBy(() -> runner.run(source, directory.resolve("timeout"), "snapshot", Axis.MEDAL))
                .isInstanceOf(IllegalStateException.class).hasMessage("ACHIEVEMENT_EXPERIMENT_TIMEOUT");
        verify(child).destroyForcibly();
        verify(process).destroyForcibly();
        runner.run(source, directory.resolve("retry"), "snapshot", Axis.MEDAL);
        runner.shutdown();
    }

    @Test void nonzeroExitUsesSafeErrorWithoutEchoingProcessOutput() throws Exception {
        var process = finishedProcess(2);
        var runner = runner(builder -> process);
        Path source = source();
        assertThatThrownBy(() -> runner.run(source, directory.resolve("failed"), "snapshot", Axis.MEDAL))
                .isInstanceOf(IllegalStateException.class).hasMessage("ACHIEVEMENT_EXPERIMENT_FAILED");
        runner.shutdown();
    }

    @Test void failedStartDoesNotExposeOperatingSystemErrorDetails() throws Exception {
        var runner = runner(builder -> { throw new IOException("sensitive path or environment"); });
        Path source = source();
        assertThatThrownBy(() -> runner.run(source, directory.resolve("failed"), "snapshot", Axis.MEDAL))
                .isInstanceOf(IllegalStateException.class).hasMessage("ACHIEVEMENT_RUNTIME_UNAVAILABLE")
                .hasNoCause();
        runner.shutdown();
    }

    @Test void interruptTerminatesProcessAndPreservesThreadInterruption() throws Exception {
        var process = finishedProcess(0);
        when(process.waitFor(anyLong(), eq(TimeUnit.MILLISECONDS))).thenThrow(new InterruptedException());
        var runner = runner(builder -> process);
        Path source = source();
        try {
            assertThatThrownBy(() -> runner.run(source, directory.resolve("interrupted"), "snapshot", Axis.MEDAL))
                    .isInstanceOf(InterruptedException.class);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
            verify(process).destroyForcibly();
        } finally {
            Thread.interrupted();
            runner.shutdown();
        }
    }

    @Test void shutdownKillsActiveProcessAndRejectsFurtherRuns() throws Exception {
        var process = finishedProcess(143);
        var waiting = new CountDownLatch(1);
        var destroyed = new CountDownLatch(1);
        when(process.waitFor(anyLong(), eq(TimeUnit.MILLISECONDS))).thenAnswer(inv -> {
            waiting.countDown();
            return destroyed.await(5, TimeUnit.SECONDS);
        });
        when(process.destroyForcibly()).thenAnswer(inv -> { destroyed.countDown(); return process; });
        var runner = runner(builder -> process);
        Path source = source();
        try (var executor = Executors.newSingleThreadExecutor()) {
            var result = executor.submit(() -> {
                assertThatThrownBy(() -> runner.run(source, directory.resolve("shutdown"), "snapshot", Axis.MEDAL))
                        .isInstanceOf(IllegalStateException.class);
            });
            assertThat(waiting.await(5, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> runner.run(source, directory.resolve("concurrent"), "snapshot", Axis.RANK))
                    .hasMessage("ACHIEVEMENT_RUNTIME_BUSY");
            runner.shutdown();
            result.get(5, TimeUnit.SECONDS);
            verify(process, atLeastOnce()).destroyForcibly();
            assertThatThrownBy(() -> runner.run(source, directory.resolve("closed"), "snapshot", Axis.RANK))
                    .hasMessage("ACHIEVEMENT_RUNTIME_CLOSED");
        }
    }

    private AchievementExperimentRunner runner(AchievementExperimentRunner.ProcessStarter starter) throws IOException {
        Files.writeString(script(), "# dummy script; the process starter is injected in tests");
        return new AchievementExperimentRunner("python3", script(), 30, Duration.ofSeconds(1), starter);
    }

    private Path script() { return directory.resolve("experiment.py").toAbsolutePath().normalize(); }

    private Path source() throws IOException {
        Path source = Files.createDirectories(directory.resolve("source"));
        Files.writeString(source.resolve("catalog.json"), "[]");
        Files.writeString(source.resolve("records.jsonl"), "");
        return source;
    }

    private Process finishedProcess(int exitValue) throws InterruptedException {
        var process = mock(Process.class);
        when(process.waitFor(anyLong(), any())).thenReturn(true);
        when(process.exitValue()).thenReturn(exitValue);
        when(process.descendants()).thenAnswer(inv -> Stream.empty());
        return process;
    }
}
