package gg.popn.application.tier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

@Service
public class AchievementBoardService {
    private final AchievementBoardPort port;
    private final int version;
    public AchievementBoardService(AchievementBoardPort port,
            @Value("${popngg.game.current-version:29}") int version) {
        this.port = port; this.version = version;
    }
    @Transactional(readOnly = true)
    public AchievementBoard load(int level, AchievementBoard.Metric metric) {
        if (level < 40 || level > 50 || metric == null) throw new IllegalArgumentException("레벨은 40~50입니다.");
        return new AchievementBoard(level, metric, Instant.now(), port.charts(level, version));
    }
}
