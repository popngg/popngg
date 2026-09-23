package gg.popn.application.analysis;

import java.util.Map;

/** Durable asynchronous submission. Neither method performs analysis. */
public interface AnalysisJobs {
    record Submission(String jobId, String status, boolean existing) {}
    Submission submit(String trigger, String requestKey);
    Map<String, Object> find(String jobId);
}
