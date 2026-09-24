package gg.popn.http.analysis;

import gg.popn.application.analysis.RatingQuery;
import gg.popn.application.analysis.RatingSnapshot.ChartRating;
import gg.popn.domain.common.ResponseCode;
import gg.popn.domain.common.ResponseMessage;
import gg.popn.http.common.response.SuccessResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/ratings")
public class RatingController {
    private final RatingQuery query;
    public RatingController(RatingQuery query){this.query=query;}

    @GetMapping("/charts")
    public ResponseEntity<?> charts(@RequestParam int level,@RequestParam(defaultValue="CPI") Metric metric) {
        if(level<48 || level>50)return ResponseEntity.badRequest().body(Map.of("error","UNSUPPORTED_LEVEL"));
        try {
            var snapshot=query.latest();
            var selected=snapshot.charts().stream().filter(c->c.level()==level)
                    .filter(c->"PUBLISHED".equals(metric==Metric.CPI?c.cpiStatus():c.spiStatus()))
                    .sorted(metric.comparator()).toList();
            var entries=new ArrayList<RankedChart>();int rank=0;
            for(var chart:selected)entries.add(new RankedChart(++rank,chart));
            var held=snapshot.charts().stream().filter(c->c.level()==level)
                    .filter(c->!"PUBLISHED".equals(metric==Metric.CPI?c.cpiStatus():c.spiStatus())).toList();
            return ResponseEntity.ok(success(new RankingResponse(snapshot.snapshotId(),snapshot.generatedAt(),snapshot.modelVersion(),
                    snapshot.modelStatus(),snapshot.minimumPlayers(),level,metric,entries,held)));
        } catch(IllegalStateException e) {return ResponseEntity.status(503).body(Map.of("error","RATINGS_NOT_READY"));}
    }

    @GetMapping("/charts/{chartId}")
    public ResponseEntity<?> chart(@PathVariable long chartId) {
        try{return query.latest().charts().stream().filter(c->c.chartId()==chartId).findFirst()
                .<ResponseEntity<?>>map(c->ResponseEntity.ok(success(c))).orElseGet(()->ResponseEntity.notFound().build());}
        catch(IllegalStateException e){return ResponseEntity.status(503).body(Map.of("error","RATINGS_NOT_READY"));}
    }
    private static <T> SuccessResponse<T> success(T data){return SuccessResponse.<T>builder().code(ResponseCode.SUCCESS).message(ResponseMessage.SUCCESS).data(data).build();}
    public enum Metric {
        CPI, SPI;
        Comparator<ChartRating> comparator(){return this==CPI
                ?Comparator.comparing(ChartRating::cpi,Comparator.nullsLast(Comparator.reverseOrder())).thenComparingLong(ChartRating::chartId)
                :Comparator.comparing(ChartRating::spi,Comparator.nullsLast(Comparator.reverseOrder())).thenComparingLong(ChartRating::chartId);}
    }
    public record RankedChart(int rank,ChartRating chart){}
    public record RankingResponse(String snapshotId,String generatedAt,String modelVersion,String modelStatus,int minimumPlayers,
                                  int level,Metric metric,List<RankedChart> rankings,List<ChartRating> held){}
}
