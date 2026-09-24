package gg.popn.application.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import gg.popn.application.analysis.AnalysisStatistics.Chart;
import gg.popn.application.analysis.RatingSnapshot.ChartRating;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;

/** Deterministic experimental model. Values are latent coefficients, not a public 0-100 scale. */
public final class CpiSpiModel {
    public static final String VERSION = "cpi-spi-experimental-v2";
    public static final int MIN_PLAYERS = 50;
    private static final int MIN_LEVEL = 48;
    private final ObjectMapper mapper;

    public CpiSpiModel(ObjectMapper mapper) { this.mapper = mapper; }

    public Map<String,Object> fit(Path directory, List<Chart> catalog) throws IOException {
        var chartById = new HashMap<Long,Chart>();
        catalog.forEach(c -> chartById.put(c.chartId(), c));
        var cpi = new ArrayList<ClearObs>();
        var spi = new ArrayList<ScoreObs>();
        var stats = new HashMap<Long,Stats>();
        try (var in = Files.newBufferedReader(directory.resolve("records.jsonl"), StandardCharsets.UTF_8)) {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.isBlank()) continue;
                var r = mapper.readValue(line, AnalysisRecord.class);
                var chart = chartById.get(r.chartId());
                if (chart == null || chart.level() < MIN_LEVEL || chart.level() > 50) continue;
                var s = stats.computeIfAbsent(r.chartId(), ignored -> new Stats());
                if (r.cpiEligible()) {
                    boolean clear = Boolean.TRUE.equals(r.cleared());
                    cpi.add(new ClearObs(r.userId(), r.chartId(), clear));
                    s.cpiUsers.add(r.userId()); s.cpiCount++; if (clear) s.clearCount++;
                }
                if (r.spiEligible()) {
                    spi.add(new ScoreObs(r.userId(), r.chartId(), r.score()));
                    s.spiUsers.add(r.userId()); s.scores.add(r.score()); s.scoreSum += r.score();
                }
            }
        }
        var eligibleCpiCharts = new HashSet<Long>();
        var eligibleSpiCharts = new HashSet<Long>();
        stats.forEach((chartId,s)->{
            if(s.cpiUsers.size()>=MIN_PLAYERS && s.cpiCount>0 && s.clearCount>0 && s.clearCount<s.cpiCount)
                eligibleCpiCharts.add(chartId);
            if(s.spiUsers.size()>=MIN_PLAYERS && !s.scores.isEmpty() && new HashSet<>(s.scores).size()>1)
                eligibleSpiCharts.add(chartId);
        });
        cpi.removeIf(o->!eligibleCpiCharts.contains(o.chartId()));
        spi.removeIf(o->!eligibleSpiCharts.contains(o.chartId()));
        var chartLevels = new HashMap<Long,Integer>();
        catalog.forEach(c -> chartLevels.put(c.chartId(),c.level()));
        var cpiDifficulty = fitCpi(cpi,chartLevels);
        var spiDifficulty = fitSpi(spi,chartLevels);
        var ratings = new ArrayList<ChartRating>();
        for (var chart : catalog) {
            if (chart.level() < MIN_LEVEL || chart.level() > 50) continue;
            var s = stats.getOrDefault(chart.chartId(), new Stats());
            var cpiReasons=new ArrayList<String>();
            if(s.cpiUsers.size()<MIN_PLAYERS)cpiReasons.add("INSUFFICIENT_PLAYERS");
            if(s.cpiCount>0 && (s.clearCount==0 || s.clearCount==s.cpiCount))cpiReasons.add("ONE_SIDED_OUTCOMES");
            var spiReasons=new ArrayList<String>();
            if(s.spiUsers.size()<MIN_PLAYERS)spiReasons.add("INSUFFICIENT_PLAYERS");
            if(!s.scores.isEmpty() && new HashSet<>(s.scores).size()==1)spiReasons.add("CONSTANT_SCORE");
            boolean cpiReady=cpiReasons.isEmpty(),spiReady=spiReasons.isEmpty();
            var sorted = s.scores.stream().mapToInt(Integer::intValue).sorted().toArray();
            ratings.add(new ChartRating(chart.chartId(),chart.songId(),chart.songName(),chart.genreName(),chart.jacketUrl(),
                    chart.level(),chart.difficulty(),chart.upper(), cpiReady?"ELIGIBLE":"HOLD",List.copyOf(cpiReasons),
                    cpiReady?round(cpiDifficulty.get(chart.chartId())):null,s.cpiUsers.size(),s.clearCount,ratio(s.clearCount,s.cpiCount),
                    spiReady?"ELIGIBLE":"HOLD",List.copyOf(spiReasons),spiReady?round(spiDifficulty.get(chart.chartId())):null,s.spiUsers.size(),
                    ratio(s.scoreSum,s.scores.size()),AnalysisStatistics.percentile(sorted,.5)));
        }
        ratings.sort(Comparator.comparingInt(ChartRating::level).thenComparing(ChartRating::chartId));
        var snapshot = new RatingSnapshot(null,Instant.now().toString(),VERSION,"EXPERIMENTAL","NOT_VALIDATED",MIN_PLAYERS,ratings);
        mapper.writerWithDefaultPrettyPrinter().writeValue(directory.resolve("ratings.json").toFile(),snapshot);
        writeCsv(directory.resolve("ratings.csv"),ratings);
        var summary = new LinkedHashMap<String,Object>();
        summary.put("ratingModelVersion",VERSION); summary.put("ratingMinimumLevel",MIN_LEVEL);
        summary.put("ratingChartCount",ratings.size());
        summary.put("eligibleCpiChartCount",ratings.stream().filter(r->"ELIGIBLE".equals(r.cpiEligibilityStatus())).count());
        summary.put("eligibleSpiChartCount",ratings.stream().filter(r->"ELIGIBLE".equals(r.spiEligibilityStatus())).count());
        return summary;
    }

    private static Map<Long,Double> fitCpi(List<ClearObs> observations,Map<Long,Integer> chartLevels) {
        var users=new HashMap<Long,Double>();var levels=new HashMap<Integer,Double>();var deviations=new HashMap<Long,Double>();
        for(var o:observations){users.putIfAbsent(o.userId(),0d);levels.putIfAbsent(chartLevels.get(o.chartId()),0d);deviations.putIfAbsent(o.chartId(),0d);}
        for(int iteration=0;iteration<60;iteration++) {
            // Block-coordinate Newton updates are intentional. Updating all three blocks from
            // the same residual is a Jacobi step on strongly coupled parameters and diverges on
            // the real, sparse user/chart graph even though each diagonal step looks valid.
            var ug=cpiSteps(observations,chartLevels,users,levels,deviations,ClearObs::userId,1d);
            double change=updatePenalized(users,ug,2d);
            var lg=cpiSteps(observations,chartLevels,users,levels,deviations,o->chartLevels.get(o.chartId()),-1d);
            change=Math.max(change,updateUnpenalized(levels,lg));
            var dg=cpiSteps(observations,chartLevels,users,levels,deviations,ClearObs::chartId,-1d);
            change=Math.max(change,updatePenalized(deviations,dg,2d));
            double mean=users.values().stream().mapToDouble(Double::doubleValue).average().orElse(0);
            users.replaceAll((k,v)->v-mean);levels.replaceAll((k,v)->v-mean);
            centerDeviations(levels,deviations,chartLevels);
            if(change<1e-7)break;
        }
        var result=new HashMap<Long,Double>();deviations.forEach((id,d)->result.put(id,levels.get(chartLevels.get(id))+d));return result;
    }

    private static Map<Long,Double> fitSpi(List<ScoreObs> observations,Map<Long,Integer> chartLevels) {
        var users=new HashMap<Long,Double>();var levels=new HashMap<Integer,Double>();var deviations=new HashMap<Long,Double>();
        double mean=observations.stream().mapToInt(ScoreObs::score).average().orElse(0);
        for(var o:observations){users.putIfAbsent(o.userId(),0d);levels.putIfAbsent(chartLevels.get(o.chartId()),0d);deviations.putIfAbsent(o.chartId(),0d);}
        for(int iteration=0;iteration<20;iteration++) {
            var us=new HashMap<Long,double[]>();var ls=new HashMap<Integer,double[]>();var ds=new HashMap<Long,double[]>();
            for(var o:observations) {
                int level=chartLevels.get(o.chartId());var u=us.computeIfAbsent(o.userId(),k->new double[2]);
                u[0]+=o.score()-mean+levels.get(level)+deviations.get(o.chartId());u[1]++;
            }
            us.forEach((id,a)->users.put(id,a[0]/(a[1]+5d)));
            for(var o:observations) {
                int level=chartLevels.get(o.chartId());var l=ls.computeIfAbsent(level,k->new double[2]);
                l[0]+=mean+users.get(o.userId())-deviations.get(o.chartId())-o.score();l[1]++;
            }
            ls.forEach((level,a)->levels.put(level,a[0]/a[1]));
            for(var o:observations){int level=chartLevels.get(o.chartId());var d=ds.computeIfAbsent(o.chartId(),k->new double[2]);d[0]+=mean+users.get(o.userId())-levels.get(level)-o.score();d[1]++;}
            ds.forEach((id,a)->deviations.put(id,a[0]/(a[1]+5d)));
            double userMean=users.values().stream().mapToDouble(Double::doubleValue).average().orElse(0);
            users.replaceAll((k,v)->v-userMean);levels.replaceAll((k,v)->v-userMean);
            centerDeviations(levels,deviations,chartLevels);
        }
        var result=new HashMap<Long,Double>();deviations.forEach((id,d)->result.put(id,levels.get(chartLevels.get(id))+d));return result;
    }
    private static <K> Map<K,double[]> cpiSteps(List<ClearObs> observations,Map<Long,Integer> chartLevels,
            Map<Long,Double> users,Map<Integer,Double> levels,Map<Long,Double> deviations,
            Function<ClearObs,K> key,double direction){
        Map<K,double[]> result=new HashMap<>();
        for(var o:observations){
            int level=chartLevels.get(o.chartId());
            double p=sigmoid(users.get(o.userId())-levels.get(level)-deviations.get(o.chartId()));
            double residual=(o.clear()?1d:0d)-p,weight=Math.max(1e-6,p*(1-p));
            add(result,key.apply(o),direction*residual,weight);
        }
        return result;
    }
    private static <K> void add(Map<K,double[]> map,K id,double gradient,double weight){var a=map.computeIfAbsent(id,k->new double[2]);a[0]+=gradient;a[1]+=weight;}
    private static <K> double updatePenalized(Map<K,Double> values,Map<K,double[]> steps,double lambda){
        double[] max={0};
        steps.forEach((id,a)->values.compute(id,(k,v)->{double step=bounded((a[0]-lambda*v)/(a[1]+lambda));max[0]=Math.max(max[0],Math.abs(step));return v+step;}));return max[0];
    }
    private static <K> double updateUnpenalized(Map<K,Double> values,Map<K,double[]> steps){
        double[] max={0};
        steps.forEach((id,a)->values.compute(id,(k,v)->{double step=bounded(a[0]/a[1]);max[0]=Math.max(max[0],Math.abs(step));return v+step;}));return max[0];
    }
    private static double bounded(double step){return Math.max(-1d,Math.min(1d,step));}
    private static void centerDeviations(Map<Integer,Double> levels,Map<Long,Double> deviations,Map<Long,Integer> chartLevels){
        var sums=new HashMap<Integer,double[]>();deviations.forEach((id,d)->{var a=sums.computeIfAbsent(chartLevels.get(id),k->new double[2]);a[0]+=d;a[1]++;});
        sums.forEach((level,a)->{double m=a[0]/a[1];deviations.replaceAll((id,d)->chartLevels.get(id).equals(level)?d-m:d);levels.compute(level,(k,v)->v+m);});
    }
    private static double sigmoid(double x){return x>=0?1/(1+Math.exp(-x)):Math.exp(x)/(1+Math.exp(x));}
    private static Double ratio(long a,long b){return b==0?null:(double)a/b;}
    private static Double round(Double v){return v==null?null:Math.rint(v*1_000_000d)/1_000_000d;}
    private static void writeCsv(Path path,List<ChartRating> rows)throws IOException {
        try(var out=Files.newBufferedWriter(path,StandardCharsets.UTF_8)){
            out.write("chartId,songId,songName,genreName,jacketUrl,level,difficulty,upper,cpiEligibilityStatus,cpiHoldReasons,cpi,cpiSampleCount,clearCount,clearRate,spiEligibilityStatus,spiHoldReasons,spi,spiSampleCount,averageScore,medianScore\n");
            for(var r:rows)out.write(csv(Arrays.asList(r.chartId(),r.songId(),r.songName(),r.genreName(),r.jacketUrl(),r.level(),r.difficulty(),r.upper(),r.cpiEligibilityStatus(),
                    String.join("|",r.cpiHoldReasons()),Objects.toString(r.cpi(),""),r.cpiSampleCount(),r.clearCount(),Objects.toString(r.clearRate(),""),r.spiEligibilityStatus(),
                    String.join("|",r.spiHoldReasons()),Objects.toString(r.spi(),""),r.spiSampleCount(),Objects.toString(r.averageScore(),""),Objects.toString(r.medianScore(),""))));
        }
    }
    private static String csv(List<?> values){return values.stream().map(v->v==null?"":v.toString()).map(v->"\""+v.replace("\"","\"\"")+"\"").collect(java.util.stream.Collectors.joining(","))+"\n";}
    private record ClearObs(long userId,long chartId,boolean clear){}
    private record ScoreObs(long userId,long chartId,int score){}
    private static final class Stats {final Set<Long> cpiUsers=new HashSet<>(),spiUsers=new HashSet<>();final List<Integer> scores=new ArrayList<>();int cpiCount,clearCount;long scoreSum;}
}
