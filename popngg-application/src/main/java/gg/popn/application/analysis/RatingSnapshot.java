package gg.popn.application.analysis;

import java.util.List;

public record RatingSnapshot(
        String snapshotId,
        String generatedAt,
        String modelVersion,
        String modelStatus,
        String publicationStatus,
        int minimumPlayers,
        List<ChartRating> charts
) {
    public record ChartRating(
            long chartId, long songId, String songName, String genreName, String jacketUrl,
            int level, int difficulty, boolean upper, boolean strictJudgement, boolean strictGauge,
            String cpiIndividualityStatus, String spiIndividualityStatus,
            String cpiEligibilityStatus, List<String> cpiHoldReasons, Double cpi,
            int cpiSampleCount, int clearCount, Double clearRate,
            String spiEligibilityStatus, List<String> spiHoldReasons, Double spi,
            int spiSampleCount, Double averageScore, Double medianScore
    ) {
        public ChartRating(long chartId,long songId,String songName,String genreName,String jacketUrl,
                int level,int difficulty,boolean upper,String cpiEligibilityStatus,List<String> cpiHoldReasons,Double cpi,
                int cpiSampleCount,int clearCount,Double clearRate,String spiEligibilityStatus,List<String> spiHoldReasons,Double spi,
                int spiSampleCount,Double averageScore,Double medianScore) {
            this(chartId,songId,songName,genreName,jacketUrl,level,difficulty,upper,false,false,
                    "NOT_CALCULATED","NOT_CALCULATED",cpiEligibilityStatus,cpiHoldReasons,cpi,cpiSampleCount,clearCount,clearRate,
                    spiEligibilityStatus,spiHoldReasons,spi,spiSampleCount,averageScore,medianScore);
        }
    }
}
