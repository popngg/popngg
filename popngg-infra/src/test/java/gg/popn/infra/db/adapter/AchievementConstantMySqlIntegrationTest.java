package gg.popn.infra.db.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import gg.popn.application.analysis.AchievementConstants;
import gg.popn.infra.analysis.AchievementConstantJdbcAdapter;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AchievementConstantMySqlIntegrationTest extends MySqlIntegrationTestSupport {
    @Test void migratesImportsAndAtomicallySwitchesTheActiveAxis(){
        var ds=mysqlDataSource();
        var flyway=Flyway.configure().dataSource(ds).locations("classpath:db/migration").cleanDisabled(false).load();
        flyway.clean();flyway.migrate();
        var jdbc=new JdbcTemplate(ds);
        jdbc.update("INSERT INTO songs(song_id,genre_name,song_name,version,created_at,updated_at) VALUES(1,'genre','song',29,NOW(),NOW())");
        jdbc.update("INSERT INTO charts(chart_id,song_id,difficulty_code,difficulty_label,level,chart_version,created_at,updated_at) VALUES(1,1,4,'EX',49,29,NOW(),NOW())");
        var adapter=new AchievementConstantJdbcAdapter(jdbc,new ObjectMapper().registerModule(new JavaTimeModule()));
        var tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        var first=request("source:1",48.75);
        var stored=tx.execute(status->adapter.importSnapshot(first));
        assertThat(stored.rowCount()).isEqualTo(1);
        var snapshot=adapter.latest(49,AchievementConstants.Axis.MEDAL);
        assertThat(snapshot.charts()).singleElement().satisfies(chart->assertThat(chart.constants()).singleElement()
                .satisfies(value->{assertThat(value.value()).isEqualTo(48.75);assertThat(value.lowerBound()).isEqualTo(48.5);}));
        assertThat(tx.execute(status->adapter.importSnapshot(first)).snapshotId()).isEqualTo(stored.snapshotId());
        assertThatThrownBy(()->tx.execute(status->adapter.importSnapshot(request("source:1",49.99))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("content changed");
        var second=tx.execute(status->adapter.importSnapshot(request("source:2",49.25)));
        assertThat(second.snapshotId()).isNotEqualTo(stored.snapshotId());
        assertThat(adapter.latest(49,AchievementConstants.Axis.MEDAL).charts().getFirst()
                .constants().getFirst().value()).isEqualTo(49.25);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM achievement_constant_snapshots WHERE active_axis='MEDAL'",Integer.class)).isEqualTo(1);
    }

    private static AchievementConstants.Import request(String source,double value){
        return new AchievementConstants.Import(source,"achievement-v1","EXPERIMENTAL",
                Instant.parse("2026-09-25T00:00:00Z"),AchievementConstants.Axis.MEDAL,List.of(
                new AchievementConstants.ImportRow(1,"song",49,"medal","CLEAR",1.25,value,List.of(48.5,49.0),
                        100,50,"EXPERIMENTAL",List.of())));
    }
}
