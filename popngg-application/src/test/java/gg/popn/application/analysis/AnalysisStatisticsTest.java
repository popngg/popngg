package gg.popn.application.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class AnalysisStatisticsTest {
    @TempDir Path dir;
    final ObjectMapper mapper=new ObjectMapper();
    @Test void preservesSourceRankAndReadsSnapshotsWithoutRank() throws Exception {
        var oldRow=record(1,1,8,95000);
        var tree=mapper.valueToTree(oldRow);
        ((com.fasterxml.jackson.databind.node.ObjectNode)tree).remove("allTimeRankCode");
        assertThat(mapper.treeToValue(tree,AnalysisRecord.class).allTimeRankCode()).isNull();
        ((com.fasterxml.jackson.databind.node.ObjectNode)tree).put("allTimeRankCode",4);
        var ranked=mapper.treeToValue(tree,AnalysisRecord.class);
        write(List.of(ranked));
        new AnalysisStatistics(mapper).analyze(dir,List.of(new AnalysisStatistics.Chart(1,1,49)),List.of(1L));
        var lines=Files.readAllLines(dir.resolve("records.csv"));
        assertThat(lines.getFirst()).endsWith(",allTimeRankCode");
        assertThat(lines.get(1)).endsWith(",4");
    }
    static AnalysisRecord record(long user,long chart,int medal,Integer score) {
        return new AnalysisRecord(user,chart,chart,49,medal,score,true,true,false,false,true,true,false,false,
                29,28,0,false,null,"2026-09-23 00:00:00",null);
    }
    @Test void usesIndependentPopulationsAndPreservesNoPlayAndTies() throws Exception {
        var rows=List.of(record(1,1,8,95000),record(2,1,7,93000),record(3,1,11,95000),
                record(4,1,13,0),record(5,1,13,91000),record(6,1,1,100001));
        write(rows);
        var summary=new AnalysisStatistics(mapper).analyze(dir,List.of(new AnalysisStatistics.Chart(1,1,49),new AnalysisStatistics.Chart(2,2,50)),List.of(1L,2L,3L,4L,5L,6L,7L));
        assertThat(summary.get("cpiRecordCount")).isEqualTo(3L);
        assertThat(summary.get("spiRecordCount")).isEqualTo(4L);
        var charts=mapper.readTree(dir.resolve("chart_stats.json").toFile());
        var c=charts.get(0);
        assertThat(c.path("clearCount").asLong()).isEqualTo(2);
        assertThat(c.path("clearRate").asDouble()).isCloseTo(2.0/3,within(.0001));
        assertThat(c.path("medianScore").asDouble()).isEqualTo(94000);
        assertThat(c.path("scoreP10").asDouble()).isEqualTo(91600);
        assertThat(c.path("oneSided").asBoolean()).isFalse();
        assertThat(charts.get(1).path("clearRate").isNull()).isTrue();
        assertThat(charts.get(1).path("scoreP99").isNull()).isTrue();
        var ecdf=mapper.readTree(dir.resolve("score_percentiles.json").toFile());
        var at95=new ArrayList<com.fasterxml.jackson.databind.JsonNode>();
        ecdf.forEach(n->{if(n.path("chartId").asInt()==1 && n.path("score").asInt()==95000)at95.add(n);});
        assertThat(at95.getFirst().path("nEqual").asInt()).isEqualTo(2);
        assertThat(at95.getFirst().path("ecdf").asDouble()).isEqualTo(1);
        assertThat(at95.getFirst().path("topInclusiveRate").asDouble()).isEqualTo(.5);
        assertThat(mapper.readTree(dir.resolve("user_stats.json").toFile())).hasSize(7);
        String before=Files.readString(dir.resolve("chart_stats.json"));
        new AnalysisStatistics(mapper).analyze(dir,List.of(new AnalysisStatistics.Chart(1,1,49),new AnalysisStatistics.Chart(2,2,50)),List.of(1L,2L,3L,4L,5L,6L,7L));
        assertThat(Files.readString(dir.resolve("chart_stats.json"))).isEqualTo(before);
    }
    @Test void allRegularClearMedalsAreEqualAndAssistsNeverBecomeFailures() {
        for(int m=1;m<=7;m++) assertThat(record(1,1,m,95000).cleared()).isTrue();
        for(int m=8;m<=10;m++) assertThat(record(1,1,m,95000).cleared()).isFalse();
        for(int m:new int[]{0,11,12,13,99}) {
            assertThat(record(1,1,m,95000).cpiEligible()).isFalse();
            assertThat(record(1,1,m,95000).spiEligible()).isTrue();
        }
        assertThat(record(1,1,8,0).spiEligible()).isTrue();
        assertThat(record(1,1,13,0).spiEligible()).isFalse();
        assertThat(record(1,1,7,-1).cpiEligible()).isTrue();
        assertThat(record(1,1,7,-1).spiEligible()).isFalse();
    }
    @Test void quarantinesOrphansDuplicatesAndDeletedCharts() throws Exception {
        var bad=new AnalysisRecord(1,1,null,null,7,95000,false,false,false,false,false,false,true,true,29,28,0,false,null,null,null);
        write(List.of(bad));
        var summary=new AnalysisStatistics(mapper).analyze(dir,List.of(),List.of());
        assertThat(summary.get("cpiRecordCount")).isEqualTo(0L);
        assertThat(summary.get("spiRecordCount")).isEqualTo(0L);
        var q=mapper.readTree(dir.resolve("data_quality.json").toFile());
        assertThat(q.path("baseExcludedRecords").asInt()).isEqualTo(1);
        assertThat(q.path("duplicateKeyRows").asInt()).isEqualTo(1);
    }
    @Test void distinguishesUniquePlayersFromRecordDenominators()throws Exception {
        write(List.of(record(1,1,7,90000),record(1,2,8,80000),record(2,2,7,100000)));
        new AnalysisStatistics(mapper).analyze(dir,List.of(new AnalysisStatistics.Chart(1,1,49),new AnalysisStatistics.Chart(2,2,49)),List.of(1L,2L));
        var level=mapper.readTree(dir.resolve("level_stats.json").toFile()).get(0);
        assertThat(level.path("playerCount").asInt()).isEqualTo(2);
        assertThat(level.path("cpiRecordCount").asInt()).isEqualTo(3);
        assertThat(level.path("clearRate").asDouble()).isCloseTo(2.0/3,within(.0001));
    }
    private void write(List<AnalysisRecord> rows)throws Exception {
        try(var out=Files.newBufferedWriter(dir.resolve("records.jsonl"))){for(var r:rows){out.write(mapper.writeValueAsString(r));out.newLine();}}
    }
}
