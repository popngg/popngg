package gg.popn.domain.coverageprobe;

/**
 * Temporary production-source probe used only to verify diff coverage enforcement.
 */
public class CoverageFailureProbe {

    public String classify(int score, boolean premium) {
        int adjustedScore = score;
        if (premium) {
            adjustedScore += 10;
        } else {
            adjustedScore -= 5;
        }
        if (adjustedScore >= 90) {
            return "excellent";
        } else if (adjustedScore >= 70) {
            return "good";
        } else if (adjustedScore >= 50) {
            return "average";
        } else if (adjustedScore >= 0) {
            return "low";
        } else {
            return "invalid";
        }
    }
}
