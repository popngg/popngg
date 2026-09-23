package gg.popn.infra.analysis;

import gg.popn.application.analysis.AnalysisJobs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class AnalysisJobStore implements AnalysisJobs {
    private final JdbcTemplate jdbc; private final boolean enabled;
    public AnalysisJobStore(JdbcTemplate jdbc,@Value("${popngg.analysis.enabled:false}") boolean enabled) {this.jdbc=jdbc;this.enabled=enabled;}
    @Override @Transactional(timeout=2)
    public Submission submit(String trigger,String requestKey) {
        if(!enabled) throw new IllegalStateException("ANALYSIS_DISABLED");
        if(!Set.of("DISCORD","ADMIN","SCHEDULED").contains(trigger) || requestKey==null || requestKey.length()>120)
            throw new IllegalArgumentException("INVALID_ANALYSIS_REQUEST");
        jdbc.queryForObject("SELECT guard_id FROM analysis_job_guard WHERE guard_id=1 FOR UPDATE",Integer.class);
        var same=jdbc.queryForList("SELECT j.job_id,j.status FROM analysis_jobs j JOIN analysis_job_requests r ON r.job_id=j.job_id WHERE r.request_key=?",requestKey);
        if(!same.isEmpty()) return submission(same.getFirst(),true);
        var active=jdbc.queryForList("SELECT job_id,status FROM analysis_jobs WHERE active_slot=1");
        if(!active.isEmpty()) {
            jdbc.update("INSERT INTO analysis_job_requests (request_key,job_id) VALUES (?,?)",requestKey,active.getFirst().get("job_id"));
            return submission(active.getFirst(),true);
        }
        String id=UUID.randomUUID().toString();
        jdbc.update("INSERT INTO analysis_jobs (job_id,request_key,trigger_type,status,active_slot,created_at) VALUES (?,?,?,'QUEUED',1,UTC_TIMESTAMP(6))",id,requestKey,trigger);
        jdbc.update("INSERT INTO analysis_job_requests (request_key,job_id) VALUES (?,?)",requestKey,id);
        return new Submission(id,"QUEUED",false);
    }
    private static Submission submission(Map<String,Object> row,boolean existing) {return new Submission((String)row.get("job_id"),(String)row.get("status"),existing);}
    @Override public Map<String,Object> find(String id) {
        var rows=jdbc.queryForList("SELECT job_id,status,trigger_type,created_at,started_at,finished_at,result_json,notification_pending,notification_attempts FROM analysis_jobs WHERE job_id=?",id);
        return rows.isEmpty()?Map.of():rows.getFirst();
    }
    public Map<String,Object> active() {
        var rows=jdbc.queryForList("SELECT job_id,trigger_type FROM analysis_jobs WHERE active_slot=1");
        return rows.isEmpty()?Map.of():rows.getFirst();
    }
    public void started(String id) {jdbc.update("UPDATE analysis_jobs SET status='RUNNING',started_at=UTC_TIMESTAMP(6) WHERE job_id=? AND active_slot=1",id);}
    public void finished(String id,String status,String result) {
        jdbc.update("UPDATE analysis_jobs SET status=?,result_json=?,finished_at=UTC_TIMESTAMP(6),active_slot=NULL,notification_pending=TRUE,notification_next_at=UTC_TIMESTAMP(6) WHERE job_id=? AND active_slot=1",status,result,id);
    }
    public List<Map<String,Object>> notifications() {
        return jdbc.queryForList("SELECT job_id,result_json FROM analysis_jobs WHERE notification_pending=TRUE AND notification_next_at<=UTC_TIMESTAMP(6) ORDER BY finished_at LIMIT 5");
    }
    public void notified(String id) {jdbc.update("UPDATE analysis_jobs SET notification_pending=FALSE WHERE job_id=?",id);}
    public void retryNotification(String id) {
        jdbc.update("UPDATE analysis_jobs SET notification_attempts=notification_attempts+1, notification_next_at=TIMESTAMPADD(SECOND,LEAST(3600,30*POW(2,LEAST(notification_attempts,7))),UTC_TIMESTAMP(6)) WHERE job_id=?",id);
    }
}
