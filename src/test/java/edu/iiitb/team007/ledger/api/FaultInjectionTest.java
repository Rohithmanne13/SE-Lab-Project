package edu.iiitb.team007.ledger.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import edu.iiitb.team007.ledger.domain.Direction;
import edu.iiitb.team007.ledger.domain.LedgerEntry;
import edu.iiitb.team007.ledger.persistence.JdbcLedgerRepository;
import edu.iiitb.team007.ledger.persistence.LedgerJdbc;
import edu.iiitb.team007.ledger.persistence.TransactionRunner;
import edu.iiitb.team007.ledger.config.LedgerProperties;
import edu.iiitb.team007.ledger.testsupport.ApiClient;
import edu.iiitb.team007.ledger.testsupport.ApiTestSupport;
import edu.iiitb.team007.ledger.testsupport.TestDatabase;
import edu.iiitb.team007.ledger.testsupport.TestKeys;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * Integration test of rollback on a simulated failure (TQ-2, NFR-SAFE-3, FR-2.5, FR-2.8): a fault injected after
 * the DEBIT entry is written and before COMMIT leaves no entries, no balance change and no idempotency record.
 * Runs on its own fresh database.
 */
class FaultInjectionTest extends ApiTestSupport {

    @DynamicPropertySource
    static void ownDatabase(DynamicPropertyRegistry registry) {
        registry.add("ledger.db-path", () -> TestDatabase.fresh("fault-injection").toString());
        TestKeys.register(registry);
    }

    @MockitoSpyBean
    private JdbcLedgerRepository ledger;

    @Autowired
    private TransactionRunner transactions;

    @Autowired
    private LedgerJdbc db;

    private final AtomicBoolean failAfterDebit = new AtomicBoolean();

    @BeforeEach
    void injectFault() {
        doAnswer(inv -> {
            LedgerEntry e = inv.getArgument(0);
            if (failAfterDebit.get() && e.direction() == Direction.CREDIT) {
                throw new IllegalStateException("injected fault after the DEBIT entry");
            }
            return inv.callRealMethod();
        }).when(ledger).insertEntry(any());
    }

    @AfterEach
    void removeFault() {
        failAfterDebit.set(false);
    }

    private long count(String table) {
        return transactions.inReadTransaction(() -> db.template().queryForObject(
                "SELECT count(*) FROM " + table, Long.class));
    }

    @Autowired
    private LedgerProperties properties;

    @Test
    void usesItsOwnDatabaseNotTheSharedOne() {
        // Regression (Stage 3 defect log D3): a subclass's own database setting was overridden by the shared one.
        assertThat(properties.dbPath()).isNotEqualTo(TestDatabase.shared().toString());
    }

    @Test
    void transferFailingAfterTheDebitEntryPersistsNothing() {
        String a = newWallet("INR");
        String b = newWallet("INR");
        fund(a, 1_000, "INR");
        long txns = count("transactions");
        long entries = count("ledger_entries");
        long records = count("idempotency_records");
        String key = newKey();

        failAfterDebit.set(true);
        assertProblem(transfer(a, b, 400, "INR", key), 500, "INTERNAL_ERROR");

        assertThat(count("transactions")).isEqualTo(txns);
        assertThat(count("ledger_entries")).isEqualTo(entries);
        assertThat(count("idempotency_records")).isEqualTo(records);
        assertThat(balance(a)).isEqualTo(1_000);
        assertThat(balance(b)).isZero();

        failAfterDebit.set(false);
        ApiClient.Response retry = transfer(a, b, 400, "INR", key);
        assertThat(retry.status()).as("the same key may be retried (FR-2.8)").isEqualTo(201);
        assertThat(balance(a)).isEqualTo(600);
        assertThat(count("ledger_entries")).isEqualTo(entries + 2);
    }

    @Test
    void depositFailingAfterTheDebitEntryPersistsNothing() {
        String a = newWallet("USD");
        long entries = count("ledger_entries");
        failAfterDebit.set(true);
        assertProblem(deposit(a, 5_000, "USD", newKey()), 500, "INTERNAL_ERROR");
        assertThat(count("ledger_entries")).isEqualTo(entries);
        assertThat(balance(a)).isZero();
    }

    @Test
    void reversalFailingAfterTheDebitEntryPersistsNothing() {
        String a = newWallet("INR");
        String b = newWallet("INR");
        fund(a, 300, "INR");
        String original = transfer(a, b, 300, "INR", newKey()).json().get("transaction_id").asText();
        long entries = count("ledger_entries");
        failAfterDebit.set(true);
        assertProblem(reverse(original, "fault test", newKey()), 500, "INTERNAL_ERROR");
        assertThat(count("ledger_entries")).isEqualTo(entries);
        assertThat(balance(a)).isZero();
        assertThat(balance(b)).isEqualTo(300);
        failAfterDebit.set(false);
        assertThat(reverse(original, "fault test", newKey()).status()).isEqualTo(201);
        assertThat(balance(a)).isEqualTo(300);
    }

    @Test
    void internalErrorsRevealNoInternals() {
        String a = newWallet("INR");
        failAfterDebit.set(true);
        ApiClient.Response r = assertProblem(deposit(a, 5, "INR", newKey()), 500, "INTERNAL_ERROR");
        assertThat(r.body()).doesNotContain("injected", "IllegalState", "insertEntry", "ledger_entries");
    }
}
