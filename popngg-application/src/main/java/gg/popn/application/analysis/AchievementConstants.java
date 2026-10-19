package gg.popn.application.analysis;

import java.time.Instant;
import java.util.List;

/** Admin-only storage boundary for experimental achievement difficulty constants. */
public interface AchievementConstants {
    Snapshot latest(int level, Axis axis);
    void validateImport(Import request);
    Stored importSnapshot(Import request);

    enum Axis { MEDAL, RANK }

    record Import(String sourceSnapshotId, String modelVersion, String modelStatus,
                  Instant generatedAt, Axis axis, List<ImportRow> constants) {}

    record ImportRow(long chartId, String songName, int level, String axis, String target, Double rawDifficulty,
                     Double difficultyConstant, List<Double> interval,
                     int playerCount, int achievedCount, String status,
                     List<String> holdReasons) {}

    record Stored(long snapshotId, String sourceSnapshotId, Axis axis, int rowCount) {}

    record Snapshot(long snapshotId, String sourceSnapshotId, String modelVersion,
                    String modelStatus, Instant generatedAt, Axis axis,
                    int level, List<Chart> charts) {}

    record Chart(long chartId, String songName, String genreName, String jacketUrl,
                 int level, int difficulty, boolean upper, boolean strictGauge,
                 boolean strictJudgement, String extraType, List<Constant> constants) {}

    record Constant(String target, Double rawDifficulty, Double value,
                    Double lowerBound, Double upperBound, int playerCount,
                    int achievedCount, String status, List<String> holdReasons) {}
}
