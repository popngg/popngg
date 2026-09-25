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
            int level, int difficulty, boolean upper, String extraType,
            boolean strictJudgement, boolean strictGauge,
            String cpiIndividualityStatus, String spiIndividualityStatus,
            String cpiEligibilityStatus, List<String> cpiHoldReasons, Double cpi,
            int cpiSampleCount, int clearCount, Double clearRate,
            String spiEligibilityStatus, List<String> spiHoldReasons, Double spi,
            int spiSampleCount, Double averageScore, Double medianScore,
            CpiIndividualityMetrics cpiIndividualityMetrics,
            SpiIndividualityMetrics spiIndividualityMetrics
    ) {
        public ChartRating(long chartId, long songId, String songName, String genreName, String jacketUrl,
                int level, int difficulty, boolean upper, String extraType,
                boolean strictJudgement, boolean strictGauge,
                String cpiIndividualityStatus, String spiIndividualityStatus,
                String cpiEligibilityStatus, List<String> cpiHoldReasons, Double cpi,
                int cpiSampleCount, int clearCount, Double clearRate,
                String spiEligibilityStatus, List<String> spiHoldReasons, Double spi,
                int spiSampleCount, Double averageScore, Double medianScore) {
            this(chartId, songId, songName, genreName, jacketUrl, level, difficulty, upper,
                    extraType, strictJudgement, strictGauge, cpiIndividualityStatus,
                    spiIndividualityStatus, cpiEligibilityStatus, cpiHoldReasons, cpi,
                    cpiSampleCount, clearCount, clearRate, spiEligibilityStatus, spiHoldReasons,
                    spi, spiSampleCount, averageScore, medianScore, null, null);
        }

        public ChartRating(long chartId,long songId,String songName,String genreName,String jacketUrl,
                int level,int difficulty,boolean upper,String cpiEligibilityStatus,List<String> cpiHoldReasons,Double cpi,
                int cpiSampleCount,int clearCount,Double clearRate,String spiEligibilityStatus,List<String> spiHoldReasons,Double spi,
                int spiSampleCount,Double averageScore,Double medianScore) {
            this(chartId,songId,songName,genreName,jacketUrl,level,difficulty,upper,"NONE",false,false,
                    "NOT_CALCULATED","NOT_CALCULATED",cpiEligibilityStatus,cpiHoldReasons,cpi,cpiSampleCount,clearCount,clearRate,
                    spiEligibilityStatus,spiHoldReasons,spi,spiSampleCount,averageScore,medianScore);
        }
    }

    public record CpiIndividualityMetrics(
            double score,
            double auc,
            double brier,
            double logLoss,
            double meanResidual,
            double skillRange,
            int clearCount,
            int failCount
    ) {
    }

    public record SpiIndividualityMetrics(
            double score,
            double spearman,
            double residualVariance,
            double mae,
            double skillRange
    ) {
    }
}
