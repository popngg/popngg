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
        var charts=List.of(new Chart(1,11,49,"Easy","Genre A","https://img/1.png",4,false),
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
    }
    private AnalysisRecord record(long user,long chart,int medal,int score){return new AnalysisRecord(user,chart,chart,49,medal,score,true,true,false,false,true,true,false,false,29,29,score,true,null,null,1L);}
    private String json(Object value)throws Exception{return mapper.writeValueAsString(value);}
}
