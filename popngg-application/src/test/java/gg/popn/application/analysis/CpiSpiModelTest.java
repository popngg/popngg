package gg.popn.application.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import gg.popn.application.analysis.AnalysisStatistics.Chart;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;

class CpiSpiModelTest {
    @TempDir Path dir;
    final ObjectMapper mapper=new ObjectMapper();
    @Test void publishesEligibleChartsAndKeepsSparseChartsOnHold()throws Exception {
        var records=new ArrayList<String>();
        for(long user=1;user<=60;user++) {
            records.add(json(record(user,1,user>20?7:8,96000-(int)user*10)));
            records.add(json(record(user,2,user>45?7:8,92000-(int)user*10)));
            if(user<=10)records.add(json(record(user,3,8,99000)));
        }
        Files.write(dir.resolve("records.jsonl"),records);
        var charts=List.of(new Chart(1,11,49,"Easy","Genre A","https://img/1.png",4,false,"SUPER_EXTRA",true,true),
                new Chart(2,12,49,"Hard","Genre B","https://img/2.png",4,false),new Chart(3,13,49,"Sparse","Genre C",null,4,false));
        new CpiSpiModel(mapper).fit(dir,charts);
        var output=mapper.readValue(dir.resolve("ratings.json").toFile(),RatingSnapshot.class);
        var easy=output.charts().get(0);var hard=output.charts().get(1);var sparse=output.charts().get(2);
        assertThat(output.modelStatus()).isEqualTo("EXPERIMENTAL");
        assertThat(output.publicationStatus()).isEqualTo("NOT_VALIDATED");
        assertThat(easy.cpiEligibilityStatus()).isEqualTo("ELIGIBLE");
        assertThat(hard.cpi()).isGreaterThan(easy.cpi());
        assertThat(hard.spi()).isGreaterThan(easy.spi());
        assertThat(sparse.cpiEligibilityStatus()).isEqualTo("HOLD");
        assertThat(sparse.cpiHoldReasons()).containsExactly("INSUFFICIENT_PLAYERS","ONE_SIDED_OUTCOMES");
        assertThat(sparse.spiHoldReasons()).containsExactly("INSUFFICIENT_PLAYERS","CONSTANT_SCORE");
        assertThat(easy.songName()).isEqualTo("Easy");
        assertThat(easy.strictJudgement()).isTrue();
        assertThat(easy.strictGauge()).isTrue();
        assertThat(easy.extraType()).isEqualTo("SUPER_EXTRA");
        assertThat(easy.cpiIndividualityStatus()).isEqualTo("NOT_CALCULATED");
        assertThat(easy.spiIndividualityStatus()).isEqualTo("NOT_CALCULATED");
    }
    @Test void penalizedRaschConvergesWithDifferentPlayerPools()throws Exception {
        var records=new ArrayList<String>();
        for(long user=1;user<=120;user++){
            if(user<=90){
                records.add(json(record(user,1,user%5==0?8:7,93000)));
                records.add(json(record(user,2,user%3==0?8:7,92000)));
            }
            if(user>=31){
                records.add(json(record(user,3,user%2==0?8:7,91000)));
                records.add(json(record(user,4,user%4==0?7:8,90000)));
            }
        }
        Files.write(dir.resolve("records.jsonl"),records);
        var charts=List.of(new Chart(1,11,49,"A","G",null,4,false),new Chart(2,12,49,"B","G",null,4,false),
                new Chart(3,13,49,"C","G",null,4,false),new Chart(4,14,49,"D","G",null,4,false));
        new CpiSpiModel(mapper).fit(dir,charts);
        var output=mapper.readValue(dir.resolve("ratings.json").toFile(),RatingSnapshot.class);
        assertThat(output.modelVersion()).isEqualTo("cpi-spi-experimental-v4");
        assertThat(output.charts()).allSatisfy(chart->{assertThat(chart.cpi()).isFinite();assertThat(Math.abs(chart.cpi())).isLessThan(10);});
        assertThat(output.charts().get(3).cpi()).isGreaterThan(output.charts().get(2).cpi());
    }
    private AnalysisRecord record(long user,long chart,int medal,int score){return new AnalysisRecord(user,chart,chart,49,medal,score,true,true,false,false,true,true,false,false,29,29,score,true,null,null,1L);}
    private String json(Object value)throws Exception{return mapper.writeValueAsString(value);}
}
