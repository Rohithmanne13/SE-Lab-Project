package edu.iiitb.team007.ledger.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import edu.iiitb.team007.ledger.persistence.LedgerJdbc;
import edu.iiitb.team007.ledger.persistence.TransactionRunner;
import edu.iiitb.team007.ledger.testsupport.IntegrationTestSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** System tests of ledger reconciliation (FR-5.1-FR-5.7, NFR-REL-1, NFR-REL-5, TQ-3). */
class ReconciliationApiTest extends IntegrationTestSupport {

    @Autowired
    private TransactionRunner transactions;

    @Autowired
    private LedgerJdbc db;

    @Test
    void reportHasEveryRequiredFieldAndPasses() {
        String a = newWallet("INR");
        String b = newWallet("INR");
        fund(a, 800, "INR");
        assertThat(transfer(a, b, 300, "INR", newKey()).status()).isEqualTo(201);

        JsonNode report = reconcile();
        assertThat(report.get("status").asText()).isEqualTo("PASSED");
        assertThat(report.get("checked_at").asText()).endsWith("Z");
        assertThat(report.get("discrepancies")).isEmpty();
        long[] counts = transactions.inReadTransaction(() -> new long[] {
            db.template().queryForObject("SELECT count(*) FROM transactions", Long.class),
            db.template().queryForObject("SELECT count(*) FROM ledger_entries", Long.class),
            db.template().queryForObject("SELECT count(*) FROM wallets", Long.class)});
        assertThat(report.get("transactions_checked").asLong()).isEqualTo(counts[0]);
        assertThat(report.get("entries_checked").asLong()).isEqualTo(counts[1]);
        assertThat(report.get("wallets_checked").asLong()).isEqualTo(counts[2]);
        assertThat(report.get("total_debits").asLong()).isEqualTo(report.get("total_credits").asLong()).isPositive();
        long perCurrency = 0;
        for (JsonNode c : report.get("totals")) {
            assertThat(c.get("currency").asText()).matches("[A-Z]{3}");
            assertThat(c.get("total_debits").asLong()).isEqualTo(c.get("total_credits").asLong());
            perCurrency += c.get("total_debits").asLong();
        }
        assertThat(perCurrency).isEqualTo(report.get("total_debits").asLong());
    }

    @Test
    void onlyAdminKeysMayReconcile() {
        assertProblem(api().get("/api/v1/admin/reconciliation", CLIENT), 403, "FORBIDDEN");
        assertProblem(api().get("/api/v1/admin/reconciliation", null), 401, "UNAUTHENTICATED");
    }

    @Test
    void reconciliationIsAConsistentSnapshotWhileTransfersContinue() throws Exception {
        String a = newWallet("INR");
        String b = newWallet("INR");
        fund(a, 10_000, "INR");
        fund(b, 10_000, "INR");
        AtomicBoolean running = new AtomicBoolean(true);
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            List<Future<Integer>> writers = new ArrayList<>();
            for (int w = 0; w < 4; w++) {
                boolean forward = w % 2 == 0;
                writers.add(pool.submit(() -> {
                    int ok = 0;
                    while (running.get()) {
                        int status = forward ? transfer(a, b, 7, "INR", newKey()).status()
                                : transfer(b, a, 7, "INR", newKey()).status();
                        if (status == 201) {
                            ok++;
                        }
                    }
                    return ok;
                }));
            }
            for (int i = 0; i < 15; i++) {
                assertThat(reconcile().get("status").asText()).isEqualTo("PASSED");
            }
            running.set(false);
            int committed = 0;
            for (Future<Integer> f : writers) {
                committed += f.get(30, TimeUnit.SECONDS);
            }
            assertThat(committed).as("transfers kept being served during reconciliation").isPositive();
        } finally {
            running.set(false);
            pool.shutdownNow();
        }
        assertThat(balance(a) + balance(b)).isEqualTo(20_000);
    }
}
