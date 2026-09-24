package gg.popn.application.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import gg.popn.application.analysis.AnalysisStatistics.Chart;
import gg.popn.application.analysis.RatingSnapshot.ChartRating;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

/** Deterministic v1 candidate model. Values are latent coefficients, not a public 0-100 scale. */
public final class CpiSpiModel {
    public static final String VERSION = "cpi-spi-candidate-v1";
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
        var cpiDifficulty = fitCpi(cpi);
        var spiDifficulty = fitSpi(spi);
        var ratings = new ArrayList<ChartRating>();
        for (var chart : catalog) {
            if (chart.level() < MIN_LEVEL || chart.level() > 50) continue;
            var s = stats.getOrDefault(chart.chartId(), new Stats());
            boolean cpiReady = s.cpiUsers.size() >= MIN_PLAYERS && s.clearCount > 0 && s.clearCount < s.cpiCount;
            boolean spiReady = s.spiUsers.size() >= MIN_PLAYERS;
            var sorted = s.scores.stream().mapToInt(Integer::intValue).sorted().toArray();
            ratings.add(new ChartRating(chart.chartId(),chart.songId(),chart.songName(),chart.genreName(),chart.jacketUrl(),
                    chart.level(),chart.difficulty(),chart.upper(), cpiReady?"PUBLISHED":"HOLD",
                    cpiReady?round(cpiDifficulty.get(chart.chartId())):null,s.cpiUsers.size(),s.clearCount,ratio(s.clearCount,s.cpiCount),
                    spiReady?"PUBLISHED":"HOLD",spiReady?round(spiDifficulty.get(chart.chartId())):null,s.spiUsers.size(),
                    ratio(s.scoreSum,s.scores.size()),AnalysisStatistics.percentile(sorted,.5)));
        }
        ratings.sort(Comparator.comparingInt(ChartRating::level).thenComparing(ChartRating::chartId));
        var snapshot = new RatingSnapshot(null,Instant.now().toString(),VERSION,"CANDIDATE",MIN_PLAYERS,ratings);
        mapper.writerWithDefaultPrettyPrinter().writeValue(directory.resolve("ratings.json").toFile(),snapshot);
        writeCsv(directory.resolve("ratings.csv"),ratings);
        var summary = new LinkedHashMap<String,Object>();
        summary.put("ratingModelVersion",VERSION); summary.put("ratingMinimumLevel",MIN_LEVEL);
        summary.put("ratingChartCount",ratings.size());
        summary.put("publishedCpiChartCount",ratings.stream().filter(r->"PUBLISHED".equals(r.cpiStatus())).count());
        summary.put("publishedSpiChartCount",ratings.stream().filter(r->"PUBLISHED".equals(r.spiStatus())).count());
        return summary;
    }

    private static Map<Long,Double> fitCpi(List<ClearObs> observations) {
        var users = new HashMap<Long,Double>(); var charts = new HashMap<Long,Double>();
        for (var o:observations) { users.putIfAbsent(o.userId(),0d); charts.putIfAbsent(o.chartId(),0d); }
        for (int iteration=0; iteration<30; iteration++) {
            var ug=new HashMap<Long,double[]>(); var cg=new HashMap<Long,double[]>();
            for (var o:observations) {
                double p=sigmoid(users.get(o.userId())-charts.get(o.chartId()));
                double residual=(o.clear()?1d:0d)-p, weight=Math.max(.01,p*(1-p));
                add(ug,o.userId(),residual,weight); add(cg,o.chartId(),-residual,weight);
            }
            update(users,ug,2d); update(charts,cg,2d);
            double mean=users.values().stream().mapToDouble(Double::doubleValue).average().orElse(0);
            users.replaceAll((k,v)->v-mean); charts.replaceAll((k,v)->v+mean);
        }
        return charts;
    }

    private static Map<Long,Double> fitSpi(List<ScoreObs> observations) {
        var users=new HashMap<Long,Double>(); var charts=new HashMap<Long,Double>();
        double mean=observations.stream().mapToInt(ScoreObs::score).average().orElse(0);
        for(var o:observations){users.putIfAbsent(o.userId(),0d);charts.putIfAbsent(o.chartId(),0d);}
        for(int iteration=0;iteration<20;iteration++) {
            var us=new HashMap<Long,double[]>(); var cs=new HashMap<Long,double[]>();
            for(var o:observations) {
                var u=us.computeIfAbsent(o.userId(),k->new double[2]);u[0]+=o.score()-mean+charts.get(o.chartId());u[1]++;
            }
            us.forEach((id,a)->users.put(id,a[0]/(a[1]+5d)));
            for(var o:observations) {
                var c=cs.computeIfAbsent(o.chartId(),k->new double[2]);c[0]+=mean+users.get(o.userId())-o.score();c[1]++;
            }
            cs.forEach((id,a)->charts.put(id,a[0]/(a[1]+5d)));
        }
        return charts;
    }
    private static void add(Map<Long,double[]> map,long id,double gradient,double weight){var a=map.computeIfAbsent(id,k->new double[2]);a[0]+=gradient;a[1]+=weight;}
    private static void update(Map<Long,Double> values,Map<Long,double[]> steps,double regularization){steps.forEach((id,a)->values.compute(id,(k,v)->v+a[0]/(a[1]+regularization)));}
    private static double sigmoid(double x){return x>=0?1/(1+Math.exp(-x)):Math.exp(x)/(1+Math.exp(x));}
    private static Double ratio(long a,long b){return b==0?null:(double)a/b;}
    private static Double round(Double v){return v==null?null:Math.rint(v*1_000_000d)/1_000_000d;}
    private static void writeCsv(Path path,List<ChartRating> rows)throws IOException {
        try(var out=Files.newBufferedWriter(path,StandardCharsets.UTF_8)){
            out.write("chartId,songId,level,cpiStatus,cpi,cpiSampleCount,clearCount,clearRate,spiStatus,spi,spiSampleCount,averageScore,medianScore\n");
            for(var r:rows)out.write(String.join(",",Long.toString(r.chartId()),Long.toString(r.songId()),Integer.toString(r.level()),r.cpiStatus(),
                    Objects.toString(r.cpi(),""),Integer.toString(r.cpiSampleCount()),Integer.toString(r.clearCount()),Objects.toString(r.clearRate(),""),
                    r.spiStatus(),Objects.toString(r.spi(),""),Integer.toString(r.spiSampleCount()),Objects.toString(r.averageScore(),""),Objects.toString(r.medianScore(),""))+"\n");
        }
    }
    private record ClearObs(long userId,long chartId,boolean clear){}
    private record ScoreObs(long userId,long chartId,int score){}
    private static final class Stats {final Set<Long> cpiUsers=new HashSet<>(),spiUsers=new HashSet<>();final List<Integer> scores=new ArrayList<>();int cpiCount,clearCount;long scoreSum;}
}
