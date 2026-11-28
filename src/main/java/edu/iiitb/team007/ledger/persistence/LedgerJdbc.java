package edu.iiitb.team007.ledger.persistence;

import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Gives repositories the JDBC template of the current unit of work. Using it outside
 * {@link TransactionRunner} is a programming error: no balance may be read outside the write transaction
 * that uses it (FR-3.2), and all SQL is parameterised (SI-1, SEC-3).
 */
@Component
public class LedgerJdbc {

    private final DataSource dataSource;
    private final JdbcTemplate jdbc;

    public LedgerJdbc(DataSource dataSource) {
        this.dataSource = dataSource;
        this.jdbc = new JdbcTemplate(dataSource);
    }

    public JdbcTemplate template() {
        if (!TransactionSynchronizationManager.hasResource(dataSource)) {
            throw new IllegalStateException("Database access outside a TransactionRunner unit of work");
        }
        return jdbc;
    }
}
