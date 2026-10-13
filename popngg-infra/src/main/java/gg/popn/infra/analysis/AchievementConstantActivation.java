package gg.popn.infra.analysis;

import gg.popn.application.analysis.AchievementConstants;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Both axes become current in one transaction, after every S3 upload has succeeded. */
@Service
public class AchievementConstantActivation {
    private final AchievementConstants constants;

    public AchievementConstantActivation(AchievementConstants constants) { this.constants = constants; }

    @Transactional
    public List<AchievementConstants.Stored> activate(AchievementConstants.Import medal,
                                                     AchievementConstants.Import rank) {
        if (medal.axis() != AchievementConstants.Axis.MEDAL || rank.axis() != AchievementConstants.Axis.RANK
                || !medal.sourceSnapshotId().equals(rank.sourceSnapshotId()))
            throw new IllegalArgumentException("Achievement axes must share a source snapshot");
        return List.of(constants.importSnapshot(medal), constants.importSnapshot(rank));
    }
}
