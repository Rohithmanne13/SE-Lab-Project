package edu.iiitb.team007.ledger.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import edu.iiitb.team007.ledger.persistence.SqlScript;
import edu.iiitb.team007.ledger.testsupport.ApiClient;
import edu.iiitb.team007.ledger.testsupport.ApiTestSupport;
import edu.iiitb.team007.ledger.testsupport.SchemaFixture;
import edu.iiitb.team007.ledger.testsupport.TestDatabase;
import edu.iiitb.team007.ledger.testsupport.TestKeys;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Tamper test of SRS Appendix D (FR-4.4, FR-5.2-FR-5.6): a row changed out of band - with the append-only
 * triggers dropped - turns the report to FAILED with the matching discrepancy (still HTTP 200); restoring the row
 * makes it PASSED again. Runs on its own database.
 */
class TamperDetectionApiTest extends ApiTestSupport {

    @DynamicPropertySource
    static void ownDatabase(DynamicPropertyRegistry registry) {
        registry.add("ledger.db-path", () -> TestDatabase.fresh("tamper").toString());
        TestKeys.register(registry);
    }

    @Autowired
    private DataSource dataSource;

    private String transferId;
    private String walletA;

    @BeforeEach
    void someActivity() {
        walletA = newWallet("INR");
        String b = newWallet("INR");
        fund(walletA, 5_000, "INR");
        ApiClient.Response r = transfer(walletA, b, 1_250, "INR", newKey());
        transferId = r.json().get("transaction_id").asText();
        assertThat(reconcile().get("status").asText()).isEqualTo("PASSED");
    }

    /** Runs SQL directly on the database file with the append-only triggers dropped, then restores them. */
    private void outOfBand(String sql, Object... args) throws Exception {
        try (Connection c = dataSource.getConnection(); Statement s = c.createStatement()) {
            s.execute("BEGIN IMMEDIATE");
            for (String name : List.of("trg_transactions_no_update", "trg_transactions_no_delete",
                    "trg_ledger_entries_no_update", "trg_ledger_entries_no_delete")) {
                s.execute("DROP TRIGGER " + name);
            }
            try (PreparedStatement p = c.prepareStatement(sql)) {
                for (int i = 0; i < args.length; i++) {
                    p.setObject(i + 1, args[i]);
                }
                assertThat(p.executeUpdate()).isEqualTo(1);
            }
            for (String statement : SqlScript.statements(SchemaFixture.v1())) {
                if (statement.startsWith("CREATE TRIGGER")) {
                    s.execute(statement);
                }
            }
            s.execute("COMMIT");
        }
    }

    private static List<String> checks(JsonNode report) {
        List<String> names = new ArrayList<>();
        report.get("discrepancies").forEach(d -> names.add(d.get("check").asText()));
        return names;
    }

    @Test
    void changingAnEntryAmountBreaksTheHashBalanceAndWalletChecks() throws Exception {
        outOfBand("UPDATE ledger_entries SET amount = amount + 1 WHERE transaction_id = ? AND direction = 'CREDIT'",
                transferId);
        ApiClient.Response r = api().get("/api/v1/admin/reconciliation", ADMIN);
        assertThat(r.status()).as("a FAILED report is still 200").isEqualTo(200);
        JsonNode report = r.json();
        assertThat(report.get("status").asText()).isEqualTo("FAILED");
        assertThat(checks(report)).contains("ENTRY_HASH", "TRANSACTION_BALANCED", "WALLET_BALANCE",
                "CURRENCY_BALANCED", "LEDGER_BALANCED");
        JsonNode balanced = null;
        for (JsonNode d : report.get("discrepancies")) {
            if (d.get("check").asText().equals("TRANSACTION_BALANCED")) {
                balanced = d;
            }
        }
        assertThat(balanced.get("transaction_id").asText()).isEqualTo(transferId);
        assertThat(balanced.get("expected").asLong()).isEqualTo(1_250);
        assertThat(balanced.get("actual").asLong()).isEqualTo(1_251);

        outOfBand("UPDATE ledger_entries SET amount = amount - 1 WHERE transaction_id = ? AND direction = 'CREDIT'",
                transferId);
        assertThat(reconcile().get("status").asText()).isEqualTo("PASSED");
    }

    @Test
    void changingAStoredBalanceIsDetected() throws Exception {
        outOfBand("UPDATE wallets SET balance = balance + 100 WHERE wallet_id = ?", walletA);
        JsonNode report = reconcile();
        assertThat(report.get("status").asText()).isEqualTo("FAILED");
        assertThat(checks(report)).containsExactly("WALLET_BALANCE");
        JsonNode d = report.get("discrepancies").get(0);
        assertThat(d.get("wallet_id").asText()).isEqualTo(walletA);
        assertThat(d.get("actual").asLong()).isEqualTo(d.get("expected").asLong() + 100);
        outOfBand("UPDATE wallets SET balance = balance - 100 WHERE wallet_id = ?", walletA);
        assertThat(reconcile().get("status").asText()).isEqualTo("PASSED");
    }

    @Test
    void changingATimestampOrPrevHashBreaksTheChain() throws Exception {
        outOfBand("UPDATE ledger_entries SET created_at = '2000-01-01T00:00:00.000Z' WHERE transaction_id = ? "
                + "AND direction = 'DEBIT'", transferId);
        assertThat(checks(reconcile())).containsExactly("ENTRY_HASH");
        String original = api().get("/api/v1/transactions/" + transferId, CLIENT).json().get("created_at").asText();
        outOfBand("UPDATE ledger_entries SET created_at = ? WHERE transaction_id = ? AND direction = 'DEBIT'",
                original, transferId);
        assertThat(reconcile().get("status").asText()).isEqualTo("PASSED");

        outOfBand("UPDATE ledger_entries SET prev_hash = ? WHERE sequence = 1", "f".repeat(64));
        assertThat(checks(reconcile())).contains("PREV_HASH", "ENTRY_HASH");
        outOfBand("UPDATE ledger_entries SET prev_hash = ? WHERE sequence = 1", "0".repeat(64));
        assertThat(reconcile().get("status").asText()).isEqualTo("PASSED");
    }

    @Test
    void aDeletedEntryLeavesASequenceGap() throws Exception {
        outOfBand("UPDATE ledger_entries SET sequence = sequence + 10 WHERE sequence = (SELECT MAX(sequence) "
                + "FROM ledger_entries)");
        JsonNode report = reconcile();
        assertThat(checks(report)).contains("SEQUENCE_GAP", "ENTRY_HASH");
        outOfBand("UPDATE ledger_entries SET sequence = sequence - 10 WHERE sequence = (SELECT MAX(sequence) "
                + "FROM ledger_entries)");
        assertThat(reconcile().get("status").asText()).isEqualTo("PASSED");
    }
}
