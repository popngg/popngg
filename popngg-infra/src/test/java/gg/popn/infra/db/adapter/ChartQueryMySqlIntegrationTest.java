package gg.popn.infra.db.adapter;

import org.junit.jupiter.api.Test;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;

class ChartQueryMySqlIntegrationTest extends MySqlIntegrationTestSupport {
    @Test
    void queriesActualCatalogMigrationWithoutChartTable() {
        var dataSource = mysqlDataSource();
        var flyway = Flyway.configure().dataSource(dataSource).locations("classpath:db/migration")
                .cleanDisabled(false).load();
        flyway.clean();
        flyway.migrate();
        ChartQueryJdbcAdapterTest.verifyQueries(new JdbcTemplate(dataSource));
    }
}
