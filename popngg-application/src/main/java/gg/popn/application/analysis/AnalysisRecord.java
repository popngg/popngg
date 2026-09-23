package gg.popn.application.analysis;

/** One observed user/chart record, including invalid rows for quality accounting. */
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
public record AnalysisRecord(long userId, long chartId, Long songId, Integer level,
        Integer medal, Integer score, boolean userExists, boolean profileExists,
        boolean bot, boolean hidden, boolean chartExists, boolean songExists,
        boolean deleted, boolean duplicate, Integer currentVersion, Integer allTimeScoreVersion,
        Integer versionScore, boolean versionScoreKnown, String lastPlayedAt,
        String recordUpdatedAt, Long lastRenewLogId) {
    public boolean baseEligible() {
        return userExists && profileExists && !bot && !hidden && chartExists && songExists
                && !deleted && !duplicate && level != null && level >= 1 && level <= 50;
    }
    @com.fasterxml.jackson.annotation.JsonProperty("cleared")
    public Boolean cleared() {
        if (medal == null || medal < 1 || medal > 10) return null;
        return medal <= 7;
    }
    @com.fasterxml.jackson.annotation.JsonProperty("cpiEligible")
    public boolean cpiEligible() { return baseEligible() && cleared() != null; }
    @com.fasterxml.jackson.annotation.JsonProperty("spiEligible")
    public boolean spiEligible() {
        // A positive score proves play independently of the medal. A zero needs a played medal.
        return baseEligible() && score != null && score >= 0 && score <= 100000
                && (score > 0 || (medal != null && medal >= 1 && medal <= 12));
    }
    public boolean played() {
        return baseEligible() && ((medal != null && medal >= 1 && medal <= 12)
                || (score != null && score > 0 && score <= 100000));
    }
}
