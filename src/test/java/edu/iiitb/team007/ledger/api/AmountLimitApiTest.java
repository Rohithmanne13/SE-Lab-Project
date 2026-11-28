package edu.iiitb.team007.ledger.api;

import static org.assertj.core.api.Assertions.assertThat;

import edu.iiitb.team007.ledger.config.LedgerProperties;
import edu.iiitb.team007.ledger.testsupport.ApiClient;
import edu.iiitb.team007.ledger.testsupport.ApiTestSupport;
import edu.iiitb.team007.ledger.testsupport.TestDatabase;
import edu.iiitb.team007.ledger.testsupport.TestKeys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * The range limit of 2^53 - 1 minor units (NFR-REL-6, FR-2.1, CI-2), on its own fresh database because it drives
 * the INR system funding account to its lower limit.
 */
class AmountLimitApiTest extends ApiTestSupport {

    private static final long MAX = 9_007_199_254_740_991L;

    @DynamicPropertySource
    static void ownDatabase(DynamicPropertyRegistry registry) {
        registry.add("ledger.db-path", () -> TestDatabase.fresh("amount-limit").toString());
        TestKeys.register(registry);
    }

    @Autowired
    private LedgerProperties properties;

    @Test
    void usesItsOwnDatabaseNotTheSharedOne() {
        // Regression (Stage 3 defect log D3): a subclass's own database setting was overridden by the shared one.
        assertThat(properties.dbPath()).isNotEqualTo(TestDatabase.shared().toString());
    }

    @Test
    void theLargestAmountRoundTripsExactlyAndNoBalanceMayExceedIt() {
        String a = newWallet("INR");
        String b = newWallet("INR");
        ApiClient.Response max = deposit(a, MAX, "INR", newKey());
        assertThat(max.status()).isEqualTo(201);
        assertThat(max.body()).contains("\"amount\":9007199254740991");
        assertThat(api().get("/api/v1/wallets/" + a, CLIENT).body()).contains("\"balance\":9007199254740991");

        assertProblem(deposit(a, 1, "INR", newKey()), 422, "AMOUNT_LIMIT_EXCEEDED");
        assertProblem(deposit(b, 1, "INR", newKey()), 422, "AMOUNT_LIMIT_EXCEEDED");
        assertThat(balance(a)).isEqualTo(MAX);
        assertThat(balance(b)).isZero();

        assertThat(transfer(a, b, MAX, "INR", newKey()).status()).isEqualTo(201);
        assertThat(balance(b)).isEqualTo(MAX);
        assertProblem(transfer(a, b, 1, "INR", newKey()), 422, "INSUFFICIENT_FUNDS");
    }

    @Test
    void transferThatWouldPushTheDestinationOverTheLimitIs422() {
        String a = newWallet("USD");
        String b = newWallet("USD");
        assertThat(deposit(b, MAX - 5, "USD", newKey()).status()).isEqualTo(201);
        assertThat(deposit(a, 5, "USD", newKey()).status()).isEqualTo(201);
        assertProblem(deposit(a, 1, "USD", newKey()), 422, "AMOUNT_LIMIT_EXCEEDED");
        assertThat(transfer(a, b, 5, "USD", newKey()).status()).isEqualTo(201);
        assertThat(balance(b)).isEqualTo(MAX);
    }
}
