package edu.iiitb.team007.ledger.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import edu.iiitb.team007.ledger.testsupport.SchemaFixture;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The schema itself enforces the data rules (DR-2, DR-3, DR-4, FR-4.2, NFR-SAFE-2): append-only triggers,
 * CHECK, UNIQUE and FOREIGN KEY constraints - on a fresh database file (TQ-5).
 */
class SchemaConstraintsTest {

    private static final String U = "11111111-1111-4111-8111-111111111111";
    private static final String W = "22222222-2222-4222-8222-222222222222";
    private static final String W2 = "44444444-4444-4444-8444-444444444444";
    private static final String T = "33333333-3333-4333-8333-333333333333";
    private static final String E = "55555555-5555-4555-8555-555555555555";
    private static final String H = "a".repeat(64);

    private Connection c;
    private Statement s;

    @BeforeEach
    void schema() throws SQLException {
        c = SchemaFixture.freshSchema();
        s = c.createStatement();
        s.execute("INSERT INTO users VALUES ('" + U + "', 'Alice', 'alice@example.com', 0, 't')");
        s.execute("INSERT INTO wallets VALUES ('" + W + "', '" + U + "', 'INR', 0, 0, 't')");
        s.execute("INSERT INTO transactions VALUES ('" + T + "', 'DEPOSIT', 'COMPLETED', 'INR', 100, NULL, NULL, "
                + "'t')");
        s.execute("INSERT INTO ledger_entries VALUES ('" + E + "', 1, '" + T + "', '" + W + "', 'CREDIT', 100, 'INR', "
                + "'t', '" + "0".repeat(64) + "', '" + H + "')");
    }

    @AfterEach
    void close() throws SQLException {
        c.close();
    }

    @Test
    void transactionsCannotBeUpdatedOrDeleted() {
        assertThatThrownBy(() -> s.execute("UPDATE transactions SET amount = 1"))
                .isInstanceOf(SQLException.class).hasMessageContaining("transactions is append-only");
        assertThatThrownBy(() -> s.execute("DELETE FROM transactions"))
                .isInstanceOf(SQLException.class).hasMessageContaining("transactions is append-only");
    }

    @Test
    void ledgerEntriesCannotBeUpdatedOrDeleted() throws SQLException {
        assertThatThrownBy(() -> s.execute("UPDATE ledger_entries SET amount = 1"))
                .isInstanceOf(SQLException.class).hasMessageContaining("ledger_entries is append-only");
        assertThatThrownBy(() -> s.execute("DELETE FROM ledger_entries"))
                .isInstanceOf(SQLException.class).hasMessageContaining("ledger_entries is append-only");
        try (ResultSet rs = s.executeQuery("SELECT amount FROM ledger_entries")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getLong(1)).isEqualTo(100);
        }
    }

    @Test
    void amountsMustBePositiveAndWithinTheLimit() {
        assertThatThrownBy(() -> s.execute("INSERT INTO transactions VALUES ('t2-000000000000000000000000000000000', "
                + "'TRANSFER', 'COMPLETED', 'INR', 0, NULL, NULL, 't')")).isInstanceOf(SQLException.class);
        assertThatThrownBy(() -> s.execute("INSERT INTO transactions VALUES ('t3-000000000000000000000000000000000', "
                + "'TRANSFER', 'COMPLETED', 'INR', 9007199254740992, NULL, NULL, 't')"))
                .isInstanceOf(SQLException.class);
    }

    @Test
    void nonSystemWalletsCannotGoNegativeButSystemWalletsCan() throws SQLException {
        assertThatThrownBy(() -> s.execute("UPDATE wallets SET balance = -1 WHERE wallet_id = '" + W + "'"))
                .isInstanceOf(SQLException.class).hasMessageContaining("CHECK");
        s.execute("INSERT INTO wallets VALUES ('" + W2 + "', '" + U + "', 'USD', 0, 1, 't')");
        s.execute("UPDATE wallets SET balance = -500 WHERE wallet_id = '" + W2 + "'");
    }

    @Test
    void emailIsUniqueIgnoringCase() {
        assertThatThrownBy(() -> s.execute("INSERT INTO users VALUES ('66666666-6666-4666-8666-666666666666', "
                + "'A', 'ALICE@Example.COM', 0, 't')")).isInstanceOf(SQLException.class).hasMessageContaining("UNIQUE");
    }

    @Test
    void oneWalletPerUserAndCurrency() {
        assertThatThrownBy(() -> s.execute("INSERT INTO wallets VALUES ('77777777-7777-4777-8777-777777777777', '"
                + U + "', 'INR', 0, 0, 't')")).isInstanceOf(SQLException.class).hasMessageContaining("UNIQUE");
    }

    @Test
    void foreignKeysAreEnforced() {
        assertThatThrownBy(() -> s.execute("INSERT INTO wallets VALUES ('88888888-8888-4888-8888-888888888888', "
                + "'99999999-9999-4999-8999-999999999999', 'INR', 0, 0, 't')"))
                .isInstanceOf(SQLException.class).hasMessageContaining("FOREIGN KEY");
    }

    @Test
    void sequenceIsUniqueAndReversalReferencesOnlyOnce() {
        assertThatThrownBy(() -> s.execute("INSERT INTO ledger_entries VALUES "
                + "('e2-000000000000000000000000000000000', 1, '" + T + "', '" + W + "', 'DEBIT', 100, 'INR', 't', '"
                + H + "', '" + "b".repeat(64) + "')"))
                .isInstanceOf(SQLException.class).hasMessageContaining("UNIQUE");
        assertThatThrownBy(() -> s.execute("INSERT INTO transactions VALUES ('r1-000000000000000000000000000000000', "
                + "'TRANSFER', 'COMPLETED', 'INR', 100, NULL, '" + T + "', 't')"))
                .isInstanceOf(SQLException.class).hasMessageContaining("CHECK");
    }

    @Test
    void idempotencyKeysAreUniqueAcrossEndpoints() throws SQLException {
        s.execute("INSERT INTO idempotency_records VALUES ('key-12345', '" + H + "', '" + T + "', 201, '{}', 't')");
        assertThatThrownBy(() -> s.execute("INSERT INTO idempotency_records VALUES ('key-12345', '" + H + "', '" + T
                + "', 201, '{}', 't')")).isInstanceOf(SQLException.class).hasMessageContaining("UNIQUE");
    }
}
