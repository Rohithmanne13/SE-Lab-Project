package edu.iiitb.team007.ledger.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import edu.iiitb.team007.ledger.testsupport.ApiClient;
import edu.iiitb.team007.ledger.testsupport.IntegrationTestSupport;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** System tests of reversals (FR-4.5-FR-4.7, BR-4, BR-7, TQ-3, TQ-4). */
class ReversalApiTest extends IntegrationTestSupport {

    private String transferId(String from, String to, long amount) {
        ApiClient.Response r = transfer(from, to, amount, "INR", newKey());
        assertThat(r.status()).as(r.body()).isEqualTo(201);
        return r.json().get("transaction_id").asText();
    }

    @Test
    void reversingATransferPostsOppositeEntriesAndLeavesTheOriginalUnchanged() {
        String a = newWallet("INR");
        String b = newWallet("INR");
        fund(a, 1_000, "INR");
        String original = transferId(a, b, 300);
        JsonNode before = api().get("/api/v1/transactions/" + original, CLIENT).json();

        ApiClient.Response r = reverse(original, "Customer dispute", newKey());
        assertThat(r.status()).isEqualTo(201);
        JsonNode rev = r.json();
        assertThat(r.header("Location")).isEqualTo("/api/v1/transactions/" + rev.get("transaction_id").asText());
        assertThat(rev.get("type").asText()).isEqualTo("REVERSAL");
        assertThat(rev.get("reverses_transaction_id").asText()).isEqualTo(original);
        assertThat(rev.get("amount").asLong()).isEqualTo(300);
        assertThat(rev.get("description").asText()).isEqualTo("Customer dispute");
        assertThat(rev.get("source_wallet_id").asText()).isEqualTo(b);
        assertThat(rev.get("destination_wallet_id").asText()).isEqualTo(a);
        assertThat(rev.get("entries")).hasSize(2);
        for (JsonNode e : rev.get("entries")) {
            JsonNode match = null;
            for (JsonNode o : before.get("entries")) {
                if (o.get("wallet_id").equals(e.get("wallet_id"))) {
                    match = o;
                }
            }
            assertThat(match).isNotNull();
            assertThat(e.get("amount")).isEqualTo(match.get("amount"));
            assertThat(e.get("direction").asText()).isNotEqualTo(match.get("direction").asText());
        }
        assertThat(balance(a)).isEqualTo(1_000);
        assertThat(balance(b)).isZero();

        JsonNode after = api().get("/api/v1/transactions/" + original, CLIENT).json();
        assertThat(after.get("reversed_by_transaction_id").asText()).isEqualTo(rev.get("transaction_id").asText());
        assertThat(after.get("entries")).isEqualTo(before.get("entries"));
        assertThat(after.get("amount")).isEqualTo(before.get("amount"));
        JsonNode reversalRead = api().get("/api/v1/transactions/" + rev.get("transaction_id").asText(), CLIENT).json();
        assertThat(reversalRead.get("reverses_transaction_id").asText()).isEqualTo(original);
    }

    @Test
    void aTransactionIsReversedAtMostOnceAndAReversalIsNotReversible() {
        String a = newWallet("INR");
        String b = newWallet("INR");
        fund(a, 500, "INR");
        String original = transferId(a, b, 100);
        String reversal = reverse(original, "first", newKey()).json().get("transaction_id").asText();
        assertProblem(reverse(original, "second", newKey()), 409, "TRANSACTION_ALREADY_REVERSED");
        assertProblem(reverse(reversal, "undo the undo", newKey()), 422, "TRANSACTION_NOT_REVERSIBLE");
        assertThat(balance(a)).isEqualTo(500);
    }

    @Test
    void aReversalThatWouldMakeAWalletNegativeIs422() {
        String a = newWallet("INR");
        String b = newWallet("INR");
        String c = newWallet("INR");
        fund(a, 100, "INR");
        String original = transferId(a, b, 100);
        assertThat(transfer(b, c, 60, "INR", newKey()).status()).isEqualTo(201);
        assertProblem(reverse(original, "too late", newKey()), 422, "INSUFFICIENT_FUNDS");
        assertThat(balance(b)).isEqualTo(40);
        assertThat(api().get("/api/v1/transactions/" + original, CLIENT).json().get("reversed_by_transaction_id")
                .isNull()).isTrue();
    }

    @Test
    void reversingADepositReturnsTheMoneyToTheSystemFundingAccount() {
        String a = newWallet("USD");
        String deposit = deposit(a, 2_500, "USD", newKey()).json().get("transaction_id").asText();
        ApiClient.Response r = reverse(deposit, "Deposit entered twice", newKey());
        assertThat(r.status()).isEqualTo(201);
        assertThat(r.json().get("source_wallet_id").asText()).isEqualTo(a);
        assertThat(r.json().get("destination_wallet_id").isNull()).isTrue();
        assertThat(balance(a)).isZero();
    }

    @Test
    void reversalIsIdempotent() {
        String a = newWallet("INR");
        String b = newWallet("INR");
        fund(a, 100, "INR");
        String original = transferId(a, b, 50);
        String key = newKey();
        ApiClient.Response first = reverse(original, "mistake", key);
        ApiClient.Response again = reverse(original, "mistake", key);
        assertThat(again.status()).isEqualTo(201);
        assertThat(again.header("Idempotent-Replayed")).isEqualTo("true");
        assertThat(again.body()).isEqualTo(first.body());
        assertProblem(reverse(original, "another reason", key), 422, "IDEMPOTENCY_KEY_REUSED");
        assertThat(balance(a)).isEqualTo(100);
    }

    @Test
    void invalidReversalRequests() {
        String a = newWallet("INR");
        String b = newWallet("INR");
        fund(a, 100, "INR");
        String original = transferId(a, b, 10);
        String url = "/api/v1/transactions/" + original + "/reversal";
        assertProblem(api().post(url, ADMIN, "{\"reason\":\"x\"}"), 400, "IDEMPOTENCY_KEY_MISSING");
        for (String body : new String[] {"{}", "{\"reason\":\"\"}", "{\"reason\":\"" + "r".repeat(256) + "\"}",
            "{\"reason\":5}", "{\"reason\":\"ok\",\"amount\":1}"}) {
            assertProblem(api().post(url, ADMIN, body, "Idempotency-Key", newKey()), 400, "VALIDATION_ERROR");
        }
        assertProblem(reverse(UUID.randomUUID().toString(), "none", newKey()), 404, "TRANSACTION_NOT_FOUND");
        assertProblem(reverse("not-an-id", "none", newKey()), 400, "INVALID_ID");
        assertThat(api().post(url, ADMIN, "{\"reason\":\"" + "r".repeat(255) + "\"}", "Idempotency-Key", newKey())
                .status()).isEqualTo(201);
    }
}
