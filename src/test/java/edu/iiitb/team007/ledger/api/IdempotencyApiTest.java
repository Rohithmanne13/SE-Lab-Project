package edu.iiitb.team007.ledger.api;

import static org.assertj.core.api.Assertions.assertThat;

import edu.iiitb.team007.ledger.persistence.LedgerJdbc;
import edu.iiitb.team007.ledger.persistence.TransactionRunner;
import edu.iiitb.team007.ledger.testsupport.ApiClient;
import edu.iiitb.team007.ledger.testsupport.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Idempotency keys (FR-2.3-FR-2.9, CI-4, NFR-REL-4, BR-9, TQ-4 invalid idempotency payloads). */
class IdempotencyApiTest extends IntegrationTestSupport {

    @Autowired
    private TransactionRunner transactions;

    @Autowired
    private LedgerJdbc db;

    private int recordsFor(String key) {
        return transactions.inReadTransaction(() -> db.template().queryForObject(
                "SELECT count(*) FROM idempotency_records WHERE idempotency_key = ?", Integer.class, key));
    }

    @Test
    void replaysReturnTheByteIdenticalStoredResponseAndPostOnlyOnce() {
        String a = newWallet("INR");
        String b = newWallet("INR");
        fund(a, 1_000, "INR");
        String key = newKey();
        ApiClient.Response first = transfer(a, b, 300, "INR", key);
        assertThat(first.status()).isEqualTo(201);
        assertThat(first.header("Idempotent-Replayed")).isNull();
        for (int i = 0; i < 5; i++) {
            ApiClient.Response again = transfer(a, b, 300, "INR", key);
            assertThat(again.status()).isEqualTo(201);
            assertThat(again.header("Idempotent-Replayed")).isEqualTo("true");
            assertThat(again.body()).isEqualTo(first.body());
            assertThat(again.header("Location")).isEqualTo(first.header("Location"));
        }
        assertThat(balance(a)).isEqualTo(700);
        assertThat(balance(b)).isEqualTo(300);
        assertThat(recordsFor(key)).isEqualTo(1);
    }

    @Test
    void keyOrderAndWhitespaceDoNotMakeADifferentRequest() {
        String a = newWallet("INR");
        String b = newWallet("INR");
        fund(a, 1_000, "INR");
        String key = newKey();
        assertThat(transfer(a, b, 10, "INR", key).status()).isEqualTo(201);
        ApiClient.Response reordered = api().post("/api/v1/transfers", CLIENT, "{ \"currency\" : \"INR\",\n"
                + " \"amount\": 10, \"destination_wallet_id\": \"" + b + "\", \"source_wallet_id\": \"" + a + "\" }",
                "Idempotency-Key", key);
        assertThat(reordered.status()).isEqualTo(201);
        assertThat(reordered.header("Idempotent-Replayed")).isEqualTo("true");
        assertThat(balance(a)).isEqualTo(990);
    }

    @Test
    void sameKeyWithADifferentBodyIs422AndChangesNothing() {
        String a = newWallet("INR");
        String b = newWallet("INR");
        fund(a, 1_000, "INR");
        String key = newKey();
        assertThat(transfer(a, b, 100, "INR", key).status()).isEqualTo(201);
        assertProblem(transfer(a, b, 200, "INR", key), 422, "IDEMPOTENCY_KEY_REUSED");
        assertProblem(transfer(b, a, 100, "INR", key), 422, "IDEMPOTENCY_KEY_REUSED");
        assertThat(balance(a)).isEqualTo(900);
        assertThat(balance(b)).isEqualTo(100);
    }

    @Test
    void keysAreUniqueAcrossAllIdempotentEndpoints() {
        String a = newWallet("INR");
        String b = newWallet("INR");
        String key = newKey();
        assertThat(deposit(a, 500, "INR", key).status()).isEqualTo(201);
        assertProblem(transfer(a, b, 100, "INR", key), 422, "IDEMPOTENCY_KEY_REUSED");
        assertProblem(deposit(b, 500, "INR", key), 422, "IDEMPOTENCY_KEY_REUSED");
        assertThat(balance(a)).isEqualTo(500);
        assertThat(balance(b)).isZero();
    }

    @Test
    void aFailedRequestStoresNoRecordSoTheSameKeyCanBeRetried() {
        String a = newWallet("INR");
        String b = newWallet("INR");
        String key = newKey();
        assertProblem(transfer(a, b, 100, "INR", key), 422, "INSUFFICIENT_FUNDS");
        assertThat(recordsFor(key)).isZero();
        fund(a, 100, "INR");
        ApiClient.Response retry = transfer(a, b, 100, "INR", key);
        assertThat(retry.status()).isEqualTo(201);
        assertThat(retry.header("Idempotent-Replayed")).isNull();
        assertThat(balance(b)).isEqualTo(100);
        assertThat(recordsFor(key)).isEqualTo(1);
    }
}
