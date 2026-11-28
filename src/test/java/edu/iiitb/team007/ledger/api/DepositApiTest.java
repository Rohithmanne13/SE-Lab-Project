package edu.iiitb.team007.ledger.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import edu.iiitb.team007.ledger.testsupport.ApiClient;
import edu.iiitb.team007.ledger.testsupport.IntegrationTestSupport;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** System tests of administrative deposits (FR-1.6, FR-1.7, FR-1.9, FR-2.3-FR-2.8, SEC-2, TQ-3, TQ-4). */
class DepositApiTest extends IntegrationTestSupport {

    private ApiClient.Response depositJson(String wallet, String json, String key) {
        return api().post("/api/v1/wallets/" + wallet + "/deposits", ADMIN, json, "Idempotency-Key", key);
    }

    @Test
    void depositDebitsTheSystemAccountAndCreditsTheWallet() {
        String wallet = newWallet("INR");
        ApiClient.Response r = depositJson(wallet,
                "{\"amount\":50000,\"currency\":\"INR\",\"description\":\"Initial funding\"}", newKey());

        assertThat(r.status()).isEqualTo(201);
        assertThat(r.header("Idempotent-Replayed")).isNull();
        JsonNode t = r.json();
        assertThat(r.header("Location")).isEqualTo("/api/v1/transactions/" + t.get("transaction_id").asText());
        assertThat(t.get("type").asText()).isEqualTo("DEPOSIT");
        assertThat(t.get("status").asText()).isEqualTo("COMPLETED");
        assertThat(t.get("amount").asLong()).isEqualTo(50_000);
        assertThat(t.get("currency").asText()).isEqualTo("INR");
        assertThat(t.get("description").asText()).isEqualTo("Initial funding");
        assertThat(t.get("source_wallet_id").isNull()).as("system funding account is hidden").isTrue();
        assertThat(t.get("destination_wallet_id").asText()).isEqualTo(wallet);
        JsonNode entries = t.get("entries");
        assertThat(entries).hasSize(2);
        assertThat(entries.get(0).get("direction").asText()).isEqualTo("DEBIT");
        assertThat(entries.get(0).get("wallet_id").isNull()).isTrue();
        assertThat(entries.get(1).get("direction").asText()).isEqualTo("CREDIT");
        assertThat(entries.get(1).get("wallet_id").asText()).isEqualTo(wallet);
        assertThat(entries.get(1).get("sequence").asLong()).isEqualTo(entries.get(0).get("sequence").asLong() + 1);
        assertThat(entries.get(1).get("prev_hash").asText()).isEqualTo(entries.get(0).get("entry_hash").asText());
        assertThat(balance(wallet)).isEqualTo(50_000);
    }

    @Test
    void depositIsIdempotent() {
        String wallet = newWallet("USD");
        String key = newKey();
        ApiClient.Response first = deposit(wallet, 700, "USD", key);
        ApiClient.Response again = deposit(wallet, 700, "USD", key);
        assertThat(again.status()).isEqualTo(201);
        assertThat(again.header("Idempotent-Replayed")).isEqualTo("true");
        assertThat(again.body()).isEqualTo(first.body());
        assertThat(balance(wallet)).isEqualTo(700);
        assertProblem(deposit(wallet, 701, "USD", key), 422, "IDEMPOTENCY_KEY_REUSED");
        assertThat(balance(wallet)).isEqualTo(700);
    }

    @Test
    void amountMustBeAPositiveJsonInteger() {
        String wallet = newWallet("INR");
        for (String amount : new String[] {"0", "-1", "-100", "10.5", "1e3", "100.0", "\"100\"", "true", "null",
            "9007199254740992", "99999999999999999999", "[1]", "{}"}) {
            ApiClient.Response r = assertProblem(depositJson(wallet, "{\"amount\":" + amount + ",\"currency\":\"INR\"}",
                    newKey()), 400, "VALIDATION_ERROR");
            assertThat(r.json().get("errors").get(0).get("field").asText()).as(amount).isEqualTo("amount");
        }
        assertThat(balance(wallet)).isZero();
    }

    @Test
    void currencyAndDescriptionRules() {
        String wallet = newWallet("INR");
        assertProblem(deposit(wallet, 10, "USD", newKey()), 422, "CURRENCY_MISMATCH");
        assertProblem(deposit(wallet, 10, "EUR", newKey()), 422, "UNSUPPORTED_CURRENCY");
        assertProblem(depositJson(wallet, "{\"amount\":10,\"currency\":\"inr\"}", newKey()), 400, "VALIDATION_ERROR");
        assertProblem(depositJson(wallet, "{\"amount\":10}", newKey()), 400, "VALIDATION_ERROR");
        assertProblem(depositJson(wallet, "{\"amount\":10,\"currency\":\"INR\",\"description\":\"" + "d".repeat(256)
                + "\"}", newKey()), 400, "VALIDATION_ERROR");
        assertThat(depositJson(wallet, "{\"amount\":10,\"currency\":\"INR\",\"description\":\"" + "d".repeat(255)
                + "\"}", newKey()).status()).isEqualTo(201);
        assertThat(balance(wallet)).isEqualTo(10);
    }

    @Test
    void idempotencyKeyIsRequiredAndCheckedBeforeTheBody() {
        String wallet = newWallet("INR");
        String url = "/api/v1/wallets/" + wallet + "/deposits";
        assertProblem(api().post(url, ADMIN, "{\"amount\":10,\"currency\":\"INR\"}"), 400, "IDEMPOTENCY_KEY_MISSING");
        assertProblem(api().post(url, ADMIN, "not json"), 400, "IDEMPOTENCY_KEY_MISSING");
        for (String bad : new String[] {"short", "has space-123", "\"quoted-key-123\"", "k".repeat(129), "key*12345"}) {
            assertProblem(depositJson(wallet, "{\"amount\":10,\"currency\":\"INR\"}", bad), 400,
                    "IDEMPOTENCY_KEY_INVALID");
        }
        assertThat(depositJson(wallet, "{\"amount\":10,\"currency\":\"INR\"}", "k".repeat(128)).status())
                .isEqualTo(201);
        assertThat(depositJson(wallet, "{\"amount\":10,\"currency\":\"INR\"}", "Ab_-1234").status()).isEqualTo(201);
    }

    @Test
    void unknownSystemOrMalformedWallet() {
        assertProblem(deposit(UUID.randomUUID().toString(), 10, "INR", newKey()), 404, "WALLET_NOT_FOUND");
        assertProblem(deposit("not-a-wallet", 10, "INR", newKey()), 400, "INVALID_ID");
    }
}
