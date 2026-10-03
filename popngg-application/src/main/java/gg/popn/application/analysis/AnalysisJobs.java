package gg.popn.application.analysis;

import java.util.Map;

/** Durable asynchronous submission. Submission methods never perform analysis inline. */
public interface AnalysisJobs {
    record Submission(String jobId, String status, boolean existing) {}
    Submission submit(String trigger, String requestKey);
    Submission submitAchievements(String trigger, String requestKey);
    Map<String, Object> find(String jobId);
}
