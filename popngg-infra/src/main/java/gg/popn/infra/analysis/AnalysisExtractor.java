package gg.popn.infra.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import gg.popn.application.analysis.AnalysisRecord;
import gg.popn.application.analysis.AnalysisStatistics.Chart;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Component
public class AnalysisExtractor {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final int maxRecords;
    public AnalysisExtractor(JdbcTemplate jdbc,ObjectMapper mapper,
            @Value("${popngg.analysis.max-records:3000000}") int maxRecords) {
        this.jdbc=jdbc; this.mapper=mapper; this.maxRecords=maxRecords;
    }
    public record Extracted(List<Chart> catalog,List<Long> users,Map<String,Object> metadata) {}
    // One consistent MVCC snapshot for counts, catalog, user eligibility, and observations.
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ,timeout=600)
    public Extracted extract(Path directory) throws IOException {
        long count=jdbc.queryForObject("SELECT COUNT(*) FROM playdata",Long.class);
        if(count>maxRecords) throw new IllegalStateException("ANALYSIS_RECORD_LIMIT_EXCEEDED");
        var metadata=new LinkedHashMap<String,Object>();
        metadata.put("snapshotAt",jdbc.queryForObject("SELECT UTC_TIMESTAMP(6)",String.class));
        metadata.put("databaseVersion",jdbc.queryForObject("SELECT VERSION()",String.class));
        metadata.put("sessionTimeZone",jdbc.queryForObject("SELECT @@session.time_zone",String.class));
        for(String table:List.of("users","user_profiles","songs","charts","chart_special_flags","playdata","playdata_history"))
            metadata.put(table+"Count",jdbc.queryForObject("SELECT COUNT(*) FROM "+table,Long.class));
        metadata.put("flyway",jdbc.queryForList("SELECT version, script, checksum, success FROM flyway_schema_history ORDER BY installed_rank"));
        metadata.put("columns",jdbc.queryForList("""
                SELECT table_name, column_name, column_type, is_nullable
                FROM information_schema.columns WHERE table_schema=DATABASE()
                AND table_name IN ('users','user_profiles','songs','charts','chart_special_flags','playdata','playdata_history')
                ORDER BY table_name, ordinal_position
                """));
        metadata.put("duplicateUserChartKeys",jdbc.queryForObject("""
                SELECT COUNT(*) FROM (SELECT user_id,chart_id FROM playdata
                GROUP BY user_id,chart_id HAVING COUNT(*)>1) d
                """,Long.class));
        metadata.put("orphanProfiles",jdbc.queryForObject("SELECT COUNT(*) FROM user_profiles p LEFT JOIN users u ON u.user_id=p.user_id WHERE u.user_id IS NULL",Long.class));
        metadata.put("scoreMeaning","stored all_time_score, not a single attempt; version reset may replace it");
        metadata.put("timestampMeaning","recordUpdatedAt is DB update time, not proven play time");
        var catalog=jdbc.query("""
                SELECT c.chart_id,c.song_id,c.level,s.song_name,s.genre_name,s.jacket_url,
                       c.difficulty_code,c.is_upper,COALESCE(csf.extra_type,'NONE'),
                       COALESCE(csf.has_strict_judgement,FALSE),
                       COALESCE(csf.has_strict_gauge,FALSE)
                FROM charts c JOIN songs s ON s.song_id=c.song_id
                LEFT JOIN chart_special_flags csf ON csf.chart_id=c.chart_id
                WHERE c.is_deleted=FALSE AND c.level BETWEEN 1 AND 50 ORDER BY c.chart_id
                """,(r,n)->new Chart(r.getLong(1),r.getLong(2),r.getInt(3),r.getString(4),
                        r.getString(5),r.getString(6),r.getInt(7),r.getBoolean(8),r.getString(9),
                        r.getBoolean(10),r.getBoolean(11)));
        var users=jdbc.queryForList("""
                SELECT u.user_id FROM users u JOIN user_profiles p ON p.user_id=u.user_id
                WHERE u.role <> 'BOT' AND p.is_hidden=FALSE ORDER BY u.user_id
                """,Long.class);
        Files.createDirectories(directory);
        mapper.writerWithDefaultPrettyPrinter().writeValue(directory.resolve("catalog.json").toFile(),catalog);
        mapper.writerWithDefaultPrettyPrinter().writeValue(directory.resolve("users.json").toFile(),users);
        long[] written={0};
        try(var out=Files.newBufferedWriter(directory.resolve("records.jsonl"),StandardCharsets.UTF_8)) {
            jdbc.query(connection -> {
                var statement=connection.prepareStatement(SQL,ResultSet.TYPE_FORWARD_ONLY,ResultSet.CONCUR_READ_ONLY);
                statement.setFetchSize(Integer.MIN_VALUE); // MySQL Connector/J streaming, no full result buffer.
                statement.setQueryTimeout(600);
                return statement;
            }, (org.springframework.jdbc.core.RowCallbackHandler) r -> {
                var row=new AnalysisRecord(r.getLong("user_id"),r.getLong("chart_id"),nullableLong(r,"song_id"),
                        nullableInt(r,"level"),nullableInt(r,"medal_code"),nullableInt(r,"all_time_score"),
                        r.getBoolean("user_exists"),r.getBoolean("profile_exists"),r.getBoolean("is_bot"),r.getBoolean("is_hidden"),
                        r.getBoolean("chart_exists"),r.getBoolean("song_exists"),r.getBoolean("is_deleted"),r.getBoolean("is_duplicate"),
                        nullableInt(r,"current_version"),nullableInt(r,"all_time_score_version"),nullableInt(r,"version_score"),
                        r.getBoolean("version_score_known"),r.getString("last_played_at"),r.getString("updated_at"),nullableLong(r,"last_renew_log_id"));
                try {out.write(mapper.writeValueAsString(row));out.newLine();written[0]++;}
                catch(IOException e) {throw new UncheckedIOException(e);}
            });
        }
        if(written[0]!=count) throw new IllegalStateException("EXTRACTION_COUNT_MISMATCH");
        metadata.put("extractedRecordCount",written[0]);
        metadata.put("querySha256",AnalysisArtifacts.sha256(SQL.getBytes(StandardCharsets.UTF_8)));
        mapper.writerWithDefaultPrettyPrinter().writeValue(directory.resolve("source_metadata.json").toFile(),metadata);
        return new Extracted(catalog,users,metadata);
    }
    private static Integer nullableInt(ResultSet r,String k)throws SQLException {int v=r.getInt(k);return r.wasNull()?null:v;}
    private static Long nullableLong(ResultSet r,String k)throws SQLException {long v=r.getLong(k);return r.wasNull()?null:v;}
    static final String SQL="""
            SELECT p.*, c.song_id,c.level, u.user_id IS NOT NULL AS user_exists,
                up.user_id IS NOT NULL AS profile_exists, COALESCE(u.role='BOT',FALSE) AS is_bot,
                COALESCE(up.is_hidden,FALSE) AS is_hidden, c.chart_id IS NOT NULL AS chart_exists,
                s.song_id IS NOT NULL AS song_exists, COALESCE(c.is_deleted,FALSE) AS is_deleted,
                d.user_id IS NOT NULL AS is_duplicate
            FROM playdata p
            LEFT JOIN users u ON u.user_id=p.user_id
            LEFT JOIN user_profiles up ON up.user_id=p.user_id
            LEFT JOIN charts c ON c.chart_id=p.chart_id
            LEFT JOIN songs s ON s.song_id=c.song_id
            LEFT JOIN (SELECT user_id,chart_id FROM playdata GROUP BY user_id,chart_id HAVING COUNT(*)>1) d
                ON d.user_id=p.user_id AND d.chart_id=p.chart_id
            ORDER BY p.user_id,p.chart_id,p.playdata_id
            """;
}
