package gg.popn.http.analysis;

import gg.popn.application.analysis.AnalysisJobs;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/admin/analysis/cpi-spi")
@PreAuthorize("hasRole('ADMIN')")
public class AnalysisController {
    private final AnalysisJobs jobs;
    public AnalysisController(AnalysisJobs jobs){this.jobs=jobs;}
    @PostMapping public ResponseEntity<?> submit(@RequestHeader(value="Idempotency-Key",required=false) String key) {
        if(key!=null && !key.matches("[a-zA-Z0-9_-]{1,100}"))return ResponseEntity.badRequest().build();
        try{return ResponseEntity.accepted().body(jobs.submit("ADMIN","admin:"+(key==null?UUID.randomUUID():key)));}
        catch(IllegalStateException e){return ResponseEntity.status(503).body(Map.of("error","ANALYSIS_UNAVAILABLE"));}
    }
    @GetMapping("/{id}") public ResponseEntity<?> find(@PathVariable String id) {
        var result=jobs.find(id);return result.isEmpty()?ResponseEntity.notFound().build():ResponseEntity.ok(result);
    }
}
