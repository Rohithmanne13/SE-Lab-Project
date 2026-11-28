package edu.iiitb.team007.ledger.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import edu.iiitb.team007.ledger.domain.ApiException;
import edu.iiitb.team007.ledger.domain.ErrorCode;
import edu.iiitb.team007.ledger.testsupport.IntegrationTestSupport;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Units of work (SI-3, FR-3.1, FR-3.2, FR-3.6, FR-5.1, NFR-REL-5): commit, rollback, the busy timeout and
 * readers that never block the writer.
 */
class TransactionRunnerTest extends IntegrationTestSupport {

    @Autowired
    private TransactionRunner transactions;

    @Autowired
    private LedgerJdbc db;

    @Autowired
    private DataSource dataSource;

    private int countUsersNamed(String name) {
        return transactions.inReadTransaction(() -> db.template().queryForObject(
                "SELECT count(*) FROM users WHERE name = ?", Integer.class, name));
    }

    private void insertUser(String name) {
        db.template().update("INSERT INTO users VALUES (?, ?, ?, 0, 't')", UUID.randomUUID().toString(), name,
                UUID.randomUUID() + "@example.com");
    }

    @Test
    void committedWorkIsVisibleAfterwards() {
        String name = "commit-" + UUID.randomUUID();
        transactions.inWriteTransaction(() -> {
            insertUser(name);
            return null;
        });
        assertThat(countUsersNamed(name)).isEqualTo(1);
    }

    @Test
    void anExceptionRollsBackEverything() {
        String name = "rollback-" + UUID.randomUUID();
        assertThatThrownBy(() -> transactions.inWriteTransaction(() -> {
            insertUser(name);
            insertUser(name);
            throw new IllegalStateException("fault after two inserts");
        })).hasMessage("fault after two inserts");
        assertThat(countUsersNamed(name)).isZero();
    }

    @Test
    void repositoriesCannotBeUsedOutsideAUnitOfWork() {
        assertThatThrownBy(() -> db.template()).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("outside a TransactionRunner");
    }

    @Test
    void nestedTransactionsAreRefused() {
        assertThatThrownBy(() -> transactions.inWriteTransaction(() -> transactions.inReadTransaction(() -> 1)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("nesting");
    }

    @Test
    void writeLockNotObtainedWithinBusyTimeoutGivesServiceBusy() throws Exception {
        try (Connection other = dataSource.getConnection(); Statement s = other.createStatement()) {
            s.execute("BEGIN IMMEDIATE");
            long start = System.nanoTime();
            assertThatThrownBy(() -> transactions.inWriteTransaction(() -> 1))
                    .isInstanceOfSatisfying(ApiException.class,
                            e -> assertThat(e.code()).isEqualTo(ErrorCode.SERVICE_BUSY));
            long waitedMs = (System.nanoTime() - start) / 1_000_000;
            assertThat(waitedMs).isGreaterThanOrEqualTo(SqliteDataSourceConfig.BUSY_TIMEOUT_MS - 100L);
            s.execute("ROLLBACK");
        }
        assertThat(transactions.inWriteTransaction(() -> 2)).isEqualTo(2);
    }

    @Test
    void anOpenReaderDoesNotBlockTheWriter() throws Exception {
        String name = "reader-" + UUID.randomUUID();
        try (Connection reader = dataSource.getConnection(); Statement s = reader.createStatement()) {
            s.execute("BEGIN DEFERRED");
            try (ResultSet rs = s.executeQuery("SELECT count(*) FROM users WHERE name = '" + name + "'")) {
                rs.next();
                assertThat(rs.getInt(1)).isZero();
            }
            long start = System.nanoTime();
            transactions.inWriteTransaction(() -> {
                insertUser(name);
                return null;
            });
            assertThat((System.nanoTime() - start) / 1_000_000).isLessThan(2000);
            try (ResultSet rs = s.executeQuery("SELECT count(*) FROM users WHERE name = '" + name + "'")) {
                rs.next();
                assertThat(rs.getInt(1)).as("the reader keeps its snapshot").isZero();
            }
            s.execute("COMMIT");
        }
        assertThat(countUsersNamed(name)).isEqualTo(1);
    }
}
