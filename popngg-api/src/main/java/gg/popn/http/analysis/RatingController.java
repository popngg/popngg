package gg.popn.http.analysis;

import gg.popn.application.analysis.RatingQuery;
import gg.popn.application.analysis.RatingSnapshot.ChartRating;
import gg.popn.application.playdata.dto.result.PlaydataQueryResults.ChartPlaydata;
import gg.popn.application.playdata.port.in.PlaydataQueryUseCase;
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
    private final PlaydataQueryUseCase playdata;
    public RatingController(RatingQuery query,PlaydataQueryUseCase playdata){this.query=query;this.playdata=playdata;}

    @GetMapping("/charts")
    public ResponseEntity<?> charts(@RequestParam int level,@RequestParam(defaultValue="CPI") Metric metric,
                                    @RequestParam(required=false) String poptomoId) {
        if(level<48 || level>50)return ResponseEntity.badRequest().body(Map.of("error","UNSUPPORTED_LEVEL"));
        if(!validPoptomoId(poptomoId))return ResponseEntity.badRequest().body(Map.of("error","INVALID_POPTOMO_ID"));
        try {
            var snapshot=query.latest();
            var userRecords=userRecords(poptomoId);
            var selected=snapshot.charts().stream().filter(c->c.level()==level)
                    .filter(c->"ELIGIBLE".equals(metric==Metric.CPI?c.cpiEligibilityStatus():c.spiEligibilityStatus()))
                    .sorted(metric.comparator()).toList();
            var entries=new ArrayList<RankedChart>();int rank=0;
            for(var chart:selected)entries.add(new RankedChart(++rank,chart,performance(poptomoId,userRecords.get(chart.chartId()))));
            var held=snapshot.charts().stream().filter(c->c.level()==level)
                    .filter(c->!"ELIGIBLE".equals(metric==Metric.CPI?c.cpiEligibilityStatus():c.spiEligibilityStatus()))
                    .map(c->new ChartWithUser(c,performance(poptomoId,userRecords.get(c.chartId())))).toList();
            return ResponseEntity.ok(success(new RankingResponse(snapshot.snapshotId(),snapshot.generatedAt(),snapshot.modelVersion(),
                    snapshot.modelStatus(),snapshot.publicationStatus(),snapshot.minimumPlayers(),level,metric,poptomoId,entries,held)));
        } catch(IllegalStateException e) {return ResponseEntity.status(503).body(Map.of("error","RATINGS_NOT_READY"));}
    }

    @GetMapping("/charts/{chartId}")
    public ResponseEntity<?> chart(@PathVariable long chartId,@RequestParam(required=false) String poptomoId) {
        if(!validPoptomoId(poptomoId))return ResponseEntity.badRequest().body(Map.of("error","INVALID_POPTOMO_ID"));
        try {
            var records=userRecords(poptomoId);
            return query.latest().charts().stream().filter(c->c.chartId()==chartId).findFirst()
                .<ResponseEntity<?>>map(c->ResponseEntity.ok(success(new ChartWithUser(c,performance(poptomoId,records.get(chartId))))))
                .orElseGet(()->ResponseEntity.notFound().build());
        }
        catch(IllegalStateException e){return ResponseEntity.status(503).body(Map.of("error","RATINGS_NOT_READY"));}
    }
    private Map<Long,ChartPlaydata> userRecords(String poptomoId){
        if(poptomoId==null)return Map.of();
        var result=new HashMap<Long,ChartPlaydata>();
        playdata.findUserPlaydata(poptomoId).playdata().forEach(r->result.put(r.chartId(),r));
        return result;
    }
    private static UserPerformance performance(String poptomoId,ChartPlaydata record){
        if(poptomoId==null)return null;
        return record==null?new UserPerformance(poptomoId,false,null,null)
                :new UserPerformance(poptomoId,true,record.allTimeBest().score(),record.medal().code());
    }
    private static boolean validPoptomoId(String value){return value==null || value.matches("^(?:\\d{4}-\\d{4}-\\d{4}|BOT-\\d+-\\d+)$");}
    private static <T> SuccessResponse<T> success(T data){return SuccessResponse.<T>builder().code(ResponseCode.SUCCESS).message(ResponseMessage.SUCCESS).data(data).build();}
    public enum Metric {
        CPI, SPI;
        Comparator<ChartRating> comparator(){return this==CPI
                ?Comparator.comparing(ChartRating::cpi,Comparator.nullsLast(Comparator.reverseOrder())).thenComparingLong(ChartRating::chartId)
                :Comparator.comparing(ChartRating::spi,Comparator.nullsLast(Comparator.reverseOrder())).thenComparingLong(ChartRating::chartId);}
    }
    public record UserPerformance(String poptomoId,boolean played,Integer allTimeScore,Integer medalCode){}
    public record ChartWithUser(ChartRating chart,UserPerformance userPerformance){}
    public record RankedChart(int rank,ChartRating chart,UserPerformance userPerformance){}
    public record RankingResponse(String snapshotId,String generatedAt,String modelVersion,String modelStatus,String publicationStatus,int minimumPlayers,
                                  int level,Metric metric,String poptomoId,List<RankedChart> rankings,List<ChartWithUser> held){}
}
