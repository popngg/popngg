package gg.popn.application.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Offline, deterministic descriptive statistics. No CPI/SPI rating is calculated. */
public final class AnalysisStatistics {
    public static final String POLICY = "cpi-spi-descriptive-v1";
    private final ObjectMapper mapper;
    public AnalysisStatistics(ObjectMapper mapper) { this.mapper = mapper; }
    public record Chart(long chartId, long songId, int level, String songName, String genreName,
                        String jacketUrl, int difficulty, boolean upper, String extraType,
                        boolean strictJudgement, boolean strictGauge) {
        public Chart(long chartId, long songId, int level) {
            this(chartId, songId, level, null, null, null, 0, false, "NONE", false, false);
        }
        public Chart(long chartId, long songId, int level, String songName, String genreName,
                     String jacketUrl, int difficulty, boolean upper) {
            this(chartId, songId, level, songName, genreName, jacketUrl, difficulty, upper, "NONE", false, false);
        }
        public Chart(long chartId, long songId, int level, String songName, String genreName,
                     String jacketUrl, int difficulty, boolean upper,
                     boolean strictJudgement, boolean strictGauge) {
            this(chartId, songId, level, songName, genreName, jacketUrl, difficulty, upper,
                    "NONE", strictJudgement, strictGauge);
        }
    }
    private record UserLevel(long userId, int level) {}

    public Map<String, Object> analyze(Path directory, List<Chart> catalog, List<Long> eligibleUsers) throws IOException {
        var charts = new TreeMap<Long, Stats>();
        var levels = new TreeMap<Integer, Stats>();
        var users = new TreeMap<Long, Stats>();
        var userLevels = new TreeMap<UserLevel, Stats>(Comparator.comparingLong(UserLevel::userId).thenComparingInt(UserLevel::level));
        var chartInfo = new HashMap<Long, Chart>();
        for (var c : catalog) {
            charts.put(c.chartId(), new Stats()); chartInfo.put(c.chartId(), c);
            levels.computeIfAbsent(c.level(), k -> new Stats()).catalogCharts++;
        }
        eligibleUsers.forEach(id -> users.put(id, new Stats()));
        var quality = new TreeMap<String, Long>();
        var references = new HashMap<UserLevel, long[]>();
        var cohorts = new TreeMap<String, Stats>();
        var cohortDates = new HashMap<String, String[]>();
        long[] rowCount = {0};
        Path records = directory.resolve("records.jsonl");
        try (var csv = Files.newBufferedWriter(directory.resolve("records.csv"), StandardCharsets.UTF_8)) {
            csv.write("userId,chartId,songId,level,cleared,medal,score,cpiEligible,spiEligible\n");
            read(records, r -> {
                rowCount[0]++;
                countQuality(quality, r);
                csv.write(String.join(",", String.valueOf(r.userId()), String.valueOf(r.chartId()), value(r.songId()),
                        value(r.level()), value(r.cleared()), value(r.medal()), value(r.score()),
                        String.valueOf(r.cpiEligible()), String.valueOf(r.spiEligible())) + "\n");
                if (!r.baseEligible()) return;
                if (!chartInfo.containsKey(r.chartId())) throw new IOException("CATALOG_RECORD_MISMATCH");
                var ul = new UserLevel(r.userId(), r.level());
                charts.get(r.chartId()).add(r);
                levels.computeIfAbsent(r.level(), k -> new Stats()).add(r);
                users.computeIfAbsent(r.userId(), k -> new Stats()).add(r);
                userLevels.computeIfAbsent(ul, k -> new Stats()).add(r);
                String cohort = r.level()+":"+r.allTimeScoreVersion()+":"+r.versionScoreKnown()+":"+(r.lastRenewLogId()!=null);
                cohorts.computeIfAbsent(cohort,k->new Stats()).add(r);
                if(r.recordUpdatedAt()!=null) {
                    var dates=cohortDates.computeIfAbsent(cohort,k->new String[]{r.recordUpdatedAt(),r.recordUpdatedAt()});
                    if(r.recordUpdatedAt().compareTo(dates[0])<0)dates[0]=r.recordUpdatedAt();
                    if(r.recordUpdatedAt().compareTo(dates[1])>0)dates[1]=r.recordUpdatedAt();
                }
                // Fixed disjoint reference/validation chart sets; no target leakage or popclass.
                if (r.cpiEligible() && reference(r.chartId())) {
                    var a = references.computeIfAbsent(ul, k -> new long[2]);
                    a[0]++; if (Boolean.TRUE.equals(r.cleared())) a[1]++;
                }
            });
        }
        var chartRows = new ArrayList<Map<String, Object>>();
        var levelRows = new ArrayList<Map<String, Object>>();
        var userRows = new ArrayList<Map<String, Object>>();
        var ulRows = new ArrayList<Map<String, Object>>();
        for (var entry : charts.entrySet()) {
            var c = chartInfo.get(entry.getKey());
            var row = entry.getValue().result();
            row.put("chartId", c.chartId()); row.put("songId", c.songId()); row.put("level", c.level());
            chartRows.add(row);
        }
        for (var entry : levels.entrySet()) {
            var row = entry.getValue().result(); row.put("level", entry.getKey());
            var peers = chartRows.stream().filter(c -> c.get("level").equals(entry.getKey())).toList();
            row.put("chartCount", peers.size());
            row.put("playedChartCount", peers.stream().filter(c -> ((Number)c.get("playerCount")).longValue() > 0).count());
            row.put("cpiChartsWith50Players", peers.stream().filter(c -> (boolean)c.get("cpiAtLeast50")).count());
            row.put("spiChartsWith50Players", peers.stream().filter(c -> (boolean)c.get("spiAtLeast50")).count());
            row.put("oneSidedChartCount", peers.stream().filter(c -> (boolean)c.get("oneSided")).count());
            row.put("equalChartMeanScore", peers.stream().filter(c -> c.get("averageScore") != null)
                    .mapToDouble(c -> ((Number)c.get("averageScore")).doubleValue()).average().stream().boxed().findFirst().orElse(null));
            for (String axis : List.of("playerCount", "cpiPlayerCount", "spiPlayerCount")) {
                int[][] ranges = {{0,0},{1,9},{10,29},{30,49},{50,99},{100,Integer.MAX_VALUE}};
                for (int[] range : ranges) row.put(axis + "_" + range[0] + "_" + (range[1] == Integer.MAX_VALUE ? "plus" : range[1]),
                        peers.stream().filter(c -> {long n=((Number)c.get(axis)).longValue(); return n>=range[0] && n<=range[1];}).count());
            }
            levelRows.add(row);
        }
        users.forEach((id, s) -> { var r=s.result(); r.put("userId", id); userRows.add(r); });
        userLevels.forEach((id, s) -> { var r=s.result(); r.put("userId",id.userId()); r.put("level",id.level()); ulRows.add(r); });
        writeTable(directory, "chart_stats", chartRows);
        writeTable(directory, "level_stats", levelRows);
        writeTable(directory, "user_stats", userRows);
        writeTable(directory, "user_level_stats", ulRows);
        var cohortRows=new ArrayList<Map<String,Object>>();
        cohorts.forEach((key,s)->{
            var parts=key.split(":"); var row=s.result();
            row.put("level",Integer.valueOf(parts[0]));row.put("allTimeScoreVersion",parts[1]);
            row.put("versionScoreKnown",Boolean.valueOf(parts[2]));row.put("hasRenewLog",Boolean.valueOf(parts[3]));
            String[] dates=cohortDates.get(key);row.put("earliestDbUpdate",dates==null?null:dates[0]);row.put("latestDbUpdate",dates==null?null:dates[1]);
            cohortRows.add(row);
        });
        writeTable(directory,"record_cohorts",cohortRows);
        // Stream ECDF rather than keeping every distinct score of every chart in memory twice.
        try (var json = mapper.getFactory().createGenerator(directory.resolve("score_percentiles.json").toFile(), com.fasterxml.jackson.core.JsonEncoding.UTF8);
             var csv = Files.newBufferedWriter(directory.resolve("score_percentiles.csv"), StandardCharsets.UTF_8)) {
            json.writeStartArray(); csv.write("chartId,level,score,nBelow,nEqual,nAbove,ecdf,topInclusiveRate\n");
            for (var entry : charts.entrySet()) {
                int[] scores = entry.getValue().sorted();
                var thresholds = new TreeSet<Integer>();
                for (int score : scores) thresholds.add(score);
                thresholds.addAll(List.of(90000,93000,95000,98000,99000));
                int lo=0, hi=0;
                for (int score : thresholds) {
                    while (lo<scores.length && scores[lo]<score) lo++;
                    hi=Math.max(hi,lo); while (hi<scores.length && scores[hi]<=score) hi++;
                    var r = new LinkedHashMap<String,Object>();
                    r.put("chartId",entry.getKey()); r.put("level",chartInfo.get(entry.getKey()).level()); r.put("score",score);
                    r.put("nBelow",lo); r.put("nEqual",hi-lo); r.put("nAbove",scores.length-hi);
                    r.put("ecdf",ratio(hi,scores.length)); r.put("topInclusiveRate",ratio(scores.length-lo,scores.length));
                    mapper.writeValue(json,r); csv.write(csvLine(r.values()));
                }
            }
            json.writeEndArray();
        }
        var readiness = new TreeMap<String, long[]>();
        read(records, r -> {
            if (!r.cpiEligible() || reference(r.chartId())) return;
            var ref=references.get(new UserLevel(r.userId(),r.level()));
            if (ref==null || ref[0]<5) { inc(quality,"readinessInsufficientReferenceRecords"); return; }
            int bin=Math.min(4,(int)(5.0*ref[1]/ref[0]));
            var a=readiness.computeIfAbsent(r.chartId()+":"+r.level()+":"+bin,k->new long[2]);
            a[0]++; if (Boolean.TRUE.equals(r.cleared())) a[1]++;
        });
        var readinessRows=new ArrayList<Map<String,Object>>();
        readiness.forEach((key,a)-> {
            var parts=key.split(":"); var r=new LinkedHashMap<String,Object>();
            r.put("chartId",Long.valueOf(parts[0])); r.put("level",Integer.valueOf(parts[1])); r.put("referenceClearRateBin",Integer.valueOf(parts[2]));
            r.put("playerCount",a[0]); r.put("clearCount",a[1]); r.put("clearRate",ratio(a[1],a[0]));
            double p=(double)a[1]/a[0], z=1.96, den=1+z*z/a[0];
            double center=(p+z*z/(2*a[0]))/den, half=z*Math.sqrt(p*(1-p)/a[0]+z*z/(4*a[0]*a[0]))/den;
            r.put("wilsonLow",center-half); r.put("wilsonHigh",center+half); readinessRows.add(r);
        });
        writeTable(directory,"cpi_readiness",readinessRows);
        quality.put("rawRecordCount",rowCount[0]);
        mapper.writerWithDefaultPrettyPrinter().writeValue(directory.resolve("data_quality.json").toFile(),quality);
        long cpi=charts.values().stream().mapToLong(s->s.cpi).sum(), spi=charts.values().stream().mapToLong(s->s.n).sum();
        var summary=new LinkedHashMap<String,Object>();
        summary.put("userCount",users.size()); summary.put("chartCount",charts.size()); summary.put("rawRecordCount",rowCount[0]);
        summary.put("cpiChartsWith50Players",charts.values().stream().filter(s->s.cpiPlayers.size()>=50).count());
        summary.put("spiChartsWith50Players",charts.values().stream().filter(s->s.spiPlayers.size()>=50).count());
        summary.put("oneSidedCpiCharts",charts.values().stream().filter(s->s.cpi>0 && (s.clear==0 || s.clear==s.cpi)).count());
        summary.put("cpiRecordCount",cpi); summary.put("spiRecordCount",spi);
        summary.put("cpiUserCount",users.values().stream().filter(s->s.cpi>0).count());
        summary.put("spiUserCount",users.values().stream().filter(s->s.n>0).count());
        StringBuilder report=new StringBuilder("# CPI / SPI 기초 분석\n\n정책: "+POLICY+". 레이팅을 계산하지 않은 보유 기록 분포입니다.\n\n");
        if(Files.exists(directory.resolve("FIXTURE_NOT_PRODUCTION.txt")))
            report.append("**합성 테스트 데이터입니다. 실제 popn.gg 사용자 통계가 아닙니다.**\n\n");
        report.append("## 레벨별 표본\n\n|레벨|채보|유저|CPI 기록|SPI 기록|클리어율|점수 중앙값|\n|---|---:|---:|---:|---:|---:|---:|\n");
        for (var r:levelRows) report.append("|%s|%s|%s|%s|%s|%s|%s|\n".formatted(r.get("level"),r.get("chartCount"),r.get("playerCount"),r.get("cpiRecordCount"),r.get("spiRecordCount"),r.get("clearRate"),r.get("medianScore")));
        report.append("\n## 해석 제한과 후속 실험\n\n미플레이는 실패가 아닙니다. EASY/LONGOFF는 CPI에서 제외되지만 유효 점수는 SPI에 포함됩니다. 50명 미만도 통계에 포함됩니다.\n\n")
                .append("이번 표본에서 CPI 50명 이상 채보는 "+summary.get("cpiChartsWith50Players")+"개, SPI 50명 이상은 "+summary.get("spiChartsWith50Players")+"개입니다. CPI 한쪽 결과만 있는 채보는 "+summary.get("oneSidedCpiCharts")+"개입니다.\n\n")
                .append("갱신 시각은 플레이 시각이 아니며, 이전 버전 기록의 이관으로 날짜가 바뀔 수 있습니다. history는 모든 시도 이력이 아닙니다. 원천 메달과 점수는 같은 플레이 결과가 아닐 수 있습니다.\n\n")
                .append("record_cohorts에서 레벨·최고점 버전·버전 점수 가용성·갱신 로그 유무별 분포와 DB 갱신 시각 범위를 비교할 수 있습니다. 이것만으로 과거 플레이 당시 실력이나 실력 상승을 입증할 수는 없습니다.\n\n")
                .append("cpi_readiness는 같은 레벨에서 분리된 기준 채보 5개 이상을 보유한 유저의 관측 clearRate 5구간 비교입니다. 구간은 CPI가 아니며 곡 선택 편향이 남습니다. 기준/검증 집합은 chartId 해시로 고정됩니다. 평탄하거나 역전된 곡선도 그대로 보존합니다.\n\n")
                .append("후속 후보: CPI는 충분한 성공/실패 및 유저-채보 연결성을 확인한 뒤 규제 로지스틱과 IRT를 비교합니다. SPI는 score_percentiles의 동점·천장 효과를 보고 경험분포, 목표 점수 달성 모델, 연속 점수 모델을 비교합니다. 이 보고서만으로 최종 모델을 선정하지 않습니다.\n");
        Files.writeString(directory.resolve("report.md"),report,StandardCharsets.UTF_8);
        mapper.writerWithDefaultPrettyPrinter().writeValue(directory.resolve("summary.json").toFile(),summary);
        return summary;
    }
    private static boolean reference(long chartId) { return (Long.hashCode(chartId * 0x9E3779B97F4A7C15L) & 1)==0; }
    private static void countQuality(Map<String,Long> q, AnalysisRecord r) {
        if (!r.userExists()) inc(q,"missingUser"); if (!r.profileExists()) inc(q,"missingProfile");
        if (r.bot()) inc(q,"bot"); if(r.hidden()) inc(q,"hidden"); if(!r.chartExists()) inc(q,"missingChart");
        if(!r.songExists()) inc(q,"missingSong"); if(r.deleted()) inc(q,"deletedChart"); if(r.duplicate()) inc(q,"duplicateKeyRows");
        if(r.level()==null || r.level()<1 || r.level()>50) inc(q,"invalidLevel");
        if(r.score()==null || r.score()<0 || r.score()>100000) inc(q,"invalidScore");
        if(r.medal()==null || r.medal()<1 || r.medal()>13) inc(q,"unknownMedal");
        if(r.lastPlayedAt()==null) inc(q,"missingLastPlayedAt");
        if(!r.versionScoreKnown()) inc(q,"unknownVersionScore");
        if(r.spiEligible() && r.versionScoreKnown() && r.versionScore()!=null) {
            inc(q,"knownVersionComparisonRecords");
            if(r.versionScore()>r.score())inc(q,"versionScoreExceedsAllTimeScore");
            if(r.versionScore()<r.score())inc(q,"versionScoreBelowAllTimeScore");
        }
        if(r.lastRenewLogId()==null) inc(q,"noRenewLog");
        inc(q,"medal_"+r.medal()+"_score_"+(r.score()==null?"null":r.score()==0?"zero":r.score()>0 && r.score()<=100000?"positive":"invalid"));
        if(r.baseEligible()) inc(q,"baseEligibleRecords"); else inc(q,"baseExcludedRecords");
        if(r.cpiEligible()) inc(q,"cpiEligibleRecords"); if(r.spiEligible()) inc(q,"spiEligibleRecords");
    }
    private static void inc(Map<String,Long> q,String k) {q.merge(k,1L,Long::sum);}
    private interface Consumer {void accept(AnalysisRecord r) throws IOException;}
    private void read(Path file,Consumer consumer) throws IOException {
        try(var reader=Files.newBufferedReader(file,StandardCharsets.UTF_8)) {
            String line; while((line=reader.readLine())!=null) if(!line.isBlank()) consumer.accept(mapper.readValue(line,AnalysisRecord.class));
        }
    }
    public void writeTable(Path dir,String name,List<Map<String,Object>> rows) throws IOException {
        mapper.writerWithDefaultPrettyPrinter().writeValue(dir.resolve(name+".json").toFile(),rows);
        try(var out=Files.newBufferedWriter(dir.resolve(name+".csv"),StandardCharsets.UTF_8)) {
            if(rows.isEmpty()) return;
            out.write(csvLine(rows.getFirst().keySet()));
            for(var r:rows) out.write(csvLine(r.values()));
        }
    }
    private static String csvLine(Collection<?> values) {
        return values.stream().map(AnalysisStatistics::value).map(s->"\""+s.replace("\"","\"\"")+"\"").collect(java.util.stream.Collectors.joining(","))+"\n";
    }
    private static String value(Object v) { return v==null?"":v.toString(); }
    private static Double ratio(long a,long b) {return b==0?null:(double)a/b;}
    public static Double percentile(int[] sorted,double q) {
        if(sorted.length==0) return null;
        double pos=(sorted.length-1)*q; int lo=(int)pos, hi=(int)Math.ceil(pos);
        return sorted[lo]+(sorted[hi]-sorted[lo])*(pos-lo);
    }
    private static final class Stats {
        long cpi,clear,observations; int catalogCharts,n; long sum;
        int[] scores=new int[8]; boolean sorted;
        final Set<Long> players=new HashSet<>(), cpiPlayers=new HashSet<>(), spiPlayers=new HashSet<>(), playedCharts=new HashSet<>();
        final long[] medals=new long[14];
        void add(AnalysisRecord r) {
            observations++;
            if(r.played()) {players.add(r.userId()); playedCharts.add(r.chartId());}
            if(r.cpiEligible()) {cpi++; cpiPlayers.add(r.userId()); if(Boolean.TRUE.equals(r.cleared())) clear++;}
            int m=r.medal()==null?0:r.medal(); medals[m>=1 && m<=13?m:0]++;
            if(r.spiEligible()) {
                spiPlayers.add(r.userId()); if(n==scores.length) scores=Arrays.copyOf(scores,n*2);
                scores[n++]=r.score(); sum+=r.score(); sorted=false;
            }
        }
        int[] sorted() { if(!sorted) {scores=Arrays.copyOf(scores,n); Arrays.sort(scores); sorted=true;} return scores; }
        Map<String,Object> result() {
            int[] a=sorted(); var r=new LinkedHashMap<String,Object>();
            r.put("playerCount",players.size()); r.put("playedChartCount",playedCharts.size());
            r.put("cpiPlayerCount",cpiPlayers.size()); r.put("spiPlayerCount",spiPlayers.size());
            r.put("cpiRecordCount",cpi); r.put("spiRecordCount",n); r.put("clearCount",clear); r.put("clearRate",ratio(clear,cpi));
            r.put("averageScore",ratio(sum,n)); r.put("medianScore",percentile(a,.5));
            for(int p:new int[]{10,25,50,75,90,95,99}) r.put("scoreP"+p,percentile(a,p/100.0));
            r.put("cpiAtLeast50",cpiPlayers.size()>=50); r.put("spiAtLeast50",spiPlayers.size()>=50);
            r.put("oneSided",cpi>0 && (clear==0 || clear==cpi)); r.put("medalObservationCount",observations);
            r.put("ambiguousRecordCount",observations-cpi);
            for(int m=0;m<=13;m++) {r.put("medal"+m+"Count",medals[m]); r.put("medal"+m+"Rate",ratio(medals[m],observations));}
            r.put("failedRate",ratio(medals[8]+medals[9]+medals[10],observations));
            r.put("bad21PlusRate",ratio(medals[7],observations)); r.put("bad6To20Rate",ratio(medals[6],observations));
            r.put("bad20OrLessRate",ratio(medals[5]+medals[6],observations)); r.put("bad5OrLessRate",ratio(medals[5],observations));
            r.put("fullComboRate",ratio(medals[2]+medals[3]+medals[4],observations)); r.put("perfectRate",ratio(medals[1],observations));
            return r;
        }
    }
    public static void main(String[] args) throws Exception {
        if(args.length!=1) throw new IllegalArgumentException("Usage: AnalysisStatistics <snapshot-directory>");
        Path dir=Path.of(args[0]); ObjectMapper mapper=new ObjectMapper();
        var charts=Arrays.asList(mapper.readValue(dir.resolve("catalog.json").toFile(),Chart[].class));
        var users=Arrays.asList(mapper.readValue(dir.resolve("users.json").toFile(),Long[].class));
        System.out.println(mapper.writeValueAsString(new AnalysisStatistics(mapper).analyze(dir,charts,users)));
    }
}
