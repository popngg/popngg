package gg.popn.http.analysis;

import gg.popn.application.analysis.AchievementConstants;
import gg.popn.application.analysis.AchievementConstantArchive;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/analysis/achievement-constants")
@PreAuthorize("hasRole('ADMIN')")
public class AchievementConstantController {
    private final AchievementConstants constants;
    private final AchievementConstantArchive archive;

    public AchievementConstantController(AchievementConstants constants, AchievementConstantArchive archive) {
        this.constants = constants; this.archive = archive;
    }

    @PostMapping("/snapshots")
    public ResponseEntity<?> importSnapshot(@RequestBody AchievementConstants.Import request) {
        try {
            constants.validateImport(request);
            String artifact = archive.archive(request);
            var stored = constants.importSnapshot(request);
            return ResponseEntity.ok(Map.of("snapshotId", stored.snapshotId(), "sourceSnapshotId", stored.sourceSnapshotId(),
                    "axis", stored.axis(), "rowCount", stored.rowCount(), "artifact", artifact));
        }
        catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("error", "INVALID_ACHIEVEMENT_SNAPSHOT"));
        }
        catch (IllegalStateException exception) {
            return ResponseEntity.status(503).body(Map.of("error", "ACHIEVEMENT_ARCHIVE_UNAVAILABLE"));
        }
    }

    @GetMapping("/latest")
    public ResponseEntity<?> latest(@RequestParam int level,
                                    @RequestParam AchievementConstants.Axis axis) {
        try { return ResponseEntity.ok(constants.latest(level, axis)); }
        catch (IllegalArgumentException exception) { return ResponseEntity.badRequest().build(); }
        catch (IllegalStateException exception) {
            return ResponseEntity.status(503).body(Map.of("error", "ACHIEVEMENT_CONSTANTS_NOT_READY"));
        }
    }
}
