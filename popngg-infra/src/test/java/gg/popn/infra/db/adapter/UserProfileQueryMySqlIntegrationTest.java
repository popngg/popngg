package gg.popn.infra.db.adapter;

import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Run the same sorting, filtering, pagination, and query-shape regressions on MySQL 8. */
@Testcontainers(disabledWithoutDocker = true)
class UserProfileQueryMySqlIntegrationTest extends UserProfileJpaAdapterTest {
    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("user_profile_query_test")
            .withUsername("popngg_test")
            .withPassword("popngg_test");

    @Override
    protected DataSource dataSource() {
        return new DriverManagerDataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
    }
}
