package edu.iiitb.team007.ledger.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import edu.iiitb.team007.ledger.testsupport.IntegrationTestSupport;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Versioned migrations applied at start-up and recorded in schema_version (SI-4). */
class MigrationRunnerTest extends IntegrationTestSupport {

    @Autowired
    private MigrationRunner migrations;

    @Autowired
    private TransactionRunner transactions;

    @Autowired
    private LedgerJdbc db;

    @Test
    void schemaIsAtVersionOneAfterStartUp() {
        assertThat(transactions.inReadTransaction(migrations::currentVersion)).isEqualTo(1);
        assertThat(transactions.inReadTransaction(() -> db.template().queryForList(
                "SELECT description FROM schema_version", String.class))).containsExactly("schema");
    }

    @Test
    void runningAgainAppliesNothing() {
        assertThat(migrations.migrate()).isZero();
    }

    @Test
    void allTablesAndTriggersExist() {
        List<String> names = transactions.inReadTransaction(() -> db.template().queryForList(
                "SELECT name FROM sqlite_master WHERE type IN ('table', 'trigger') ORDER BY name", String.class));
        assertThat(names).contains("users", "wallets", "transactions", "ledger_entries", "idempotency_records",
                "schema_version", "trg_transactions_no_update", "trg_transactions_no_delete",
                "trg_ledger_entries_no_update", "trg_ledger_entries_no_delete");
    }

    @Test
    void scriptsAreFoundAndOrdered() {
        assertThat(MigrationRunner.load()).extracting(MigrationRunner.Migration::version).containsExactly(1);
    }
}
