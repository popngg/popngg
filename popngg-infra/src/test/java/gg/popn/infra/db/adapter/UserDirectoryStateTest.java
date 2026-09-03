package gg.popn.infra.db.adapter;

import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.*;

class UserDirectoryStateTest {
    private JdbcTemplate jdbc;
    private TransactionTemplate transaction;

    protected DataSource dataSource() {
        return new DriverManagerDataSource("jdbc:h2:mem:directory-state;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
    }

    @BeforeEach
    void setUp() {
        DataSource source = dataSource();
        jdbc = new JdbcTemplate(source);
        transaction = new TransactionTemplate(new DataSourceTransactionManager(source));
        UserDirectoryTestSchema.create(jdbc);
        jdbc.execute("DROP TABLE IF EXISTS playdata");
        jdbc.execute("DROP TABLE IF EXISTS charts");
        jdbc.execute("CREATE TABLE charts(chart_id BIGINT PRIMARY KEY, level INT, is_deleted BOOLEAN)");
        jdbc.execute("CREATE TABLE playdata(user_id BIGINT, chart_id BIGINT, current_version INT, medal_code INT)");
        jdbc.update("INSERT INTO charts VALUES(1,48,FALSE),(2,49,FALSE),(3,50,TRUE),(4,50,FALSE)");
        jdbc.update("INSERT INTO playdata VALUES(10,1,29,11),(10,2,29,12),(10,3,29,1),(10,4,28,1),(20,1,29,8)");
    }

    @Test
    void refreshRespectsVersionDeletionMedalAndRemovesStaleRows() {
        transaction.executeWithoutResult(status -> {
            UserDirectoryState.invalidate(jdbc);
            UserDirectoryState.refreshAll(jdbc);
        });
        assertThat(level(10, 29)).isEqualTo(49);
        assertThat(level(10, 28)).isEqualTo(50);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_clear_levels WHERE user_id=20", Integer.class)).isZero();
        transaction.executeWithoutResult(status -> {
            UserDirectoryState.invalidate(jdbc);
            jdbc.update("UPDATE playdata SET medal_code=8 WHERE user_id=10 AND current_version=29");
            UserDirectoryState.refreshUser(jdbc, 10);
        });
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_clear_levels WHERE current_version=29", Integer.class)).isZero();
        assertThat(level(10, 28)).isEqualTo(50);
    }

    @Test
    void failedRenewalRollsBackSourceSummaryAndInvalidationTogether() {
        UserDirectoryState.refreshAll(jdbc);
        transaction.executeWithoutResult(status -> {
            UserDirectoryState.invalidate(jdbc);
            jdbc.update("UPDATE charts SET level=40 WHERE chart_id=2");
            UserDirectoryState.refreshUser(jdbc, 10);
            assertThat(level(10, 29)).isEqualTo(48);
            status.setRollbackOnly();
        });
        assertThat(level(10, 29)).isEqualTo(49);
        assertThat(jdbc.queryForObject("SELECT revision FROM user_directory_revision WHERE id=1", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT level FROM charts WHERE chart_id=2", Integer.class)).isEqualTo(49);
    }

    @Test
    void startupReconcilesChangesMadeDuringRollback() {
        UserDirectoryState.refreshAll(jdbc);
        jdbc.update("UPDATE charts SET is_deleted=TRUE WHERE chart_id=2");
        var reconciler = new UserDirectoryReconciler(jdbc,
                new DataSourceTransactionManager(jdbc.getDataSource()));
        reconciler.run(null);
        assertThat(level(10, 29)).isEqualTo(48);
        assertThat(jdbc.queryForObject("SELECT revision FROM user_directory_revision WHERE id=1", Long.class)).isEqualTo(2);
    }

    private Integer level(long user, int version) {
        return jdbc.queryForObject("SELECT clear_level FROM user_clear_levels WHERE user_id=? AND current_version=?",
                Integer.class, user, version);
    }
}
