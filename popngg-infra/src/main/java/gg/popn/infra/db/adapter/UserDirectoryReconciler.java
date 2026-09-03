package gg.popn.infra.db.adapter;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** Reconcile changes made by an older application during rollback or catalog SQL migration. */
@Component
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class UserDirectoryReconciler implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;

    public UserDirectoryReconciler(JdbcTemplate jdbc, PlatformTransactionManager manager) {
        this.jdbc = jdbc;
        this.transaction = new TransactionTemplate(manager);
        this.transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    }

    @Override
    public void run(ApplicationArguments args) {
        transaction.executeWithoutResult(status -> {
            UserDirectoryState.invalidate(jdbc);
            UserDirectoryState.refreshAll(jdbc);
        });
    }
}
