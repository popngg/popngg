package gg.popn.application.tier;

import java.util.List;

public interface AchievementBoardPort {
    List<AchievementBoard.Chart> charts(int level, int gameVersion);
}
