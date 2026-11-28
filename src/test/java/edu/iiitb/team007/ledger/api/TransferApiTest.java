package edu.iiitb.team007.ledger.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import edu.iiitb.team007.ledger.service.SystemAccounts;
import edu.iiitb.team007.ledger.persistence.TransactionRunner;
import edu.iiitb.team007.ledger.testsupport.ApiClient;
import edu.iiitb.team007.ledger.testsupport.IntegrationTestSupport;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** System and negative tests of transfers (FR-2.1, FR-2.2, FR-2.10, BR-2, BR-3, TQ-3, TQ-4, SEC-2). */
class TransferApiTest extends IntegrationTestSupport {

    @Autowired
    private SystemAccounts systemAccounts;

    @Autowired
    private TransactionRunner transactions;

    private ApiClient.Response transferJson(String json) {
        return api().post("/api/v1/transfers", CLIENT, json, "Idempotency-Key", newKey());
    }

    @Test
    void transferMovesMoneyAndReturnsTheFullTransaction() {
        String alice = newWallet("INR");
        String bob = newWallet("INR");
        fund(alice, 50_000, "INR");
        ApiClient.Response r = api().post("/api/v1/transfers", CLIENT, "{\"source_wallet_id\":\"" + alice
                + "\",\"destination_wallet_id\":\"" + bob + "\",\"amount\":12550,\"currency\":\"INR\","
                + "\"description\":\"Dinner\"}", "Idempotency-Key", newKey());

        assertThat(r.status()).isEqualTo(201);
        JsonNode t = r.json();
        assertThat(t.get("transaction_id").asText()).hasSize(36);
        assertThat(t.get("type").asText()).isEqualTo("TRANSFER");
        assertThat(t.get("status").asText()).isEqualTo("COMPLETED");
        assertThat(t.get("source_wallet_id").asText()).isEqualTo(alice);
        assertThat(t.get("destination_wallet_id").asText()).isEqualTo(bob);
        assertThat(t.get("amount").isIntegralNumber()).isTrue();
        assertThat(t.get("amount").asLong()).isEqualTo(12_550);
        assertThat(t.get("currency").asText()).isEqualTo("INR");
        assertThat(t.get("description").asText()).isEqualTo("Dinner");
        assertThat(t.get("created_at").asText()).endsWith("Z");
        assertThat(t.get("entries")).hasSize(2);
        assertThat(t.get("entries").get(0).get("direction").asText()).isEqualTo("DEBIT");
        assertThat(t.get("entries").get(0).get("wallet_id").asText()).isEqualTo(alice);
        assertThat(t.get("entries").get(1).get("direction").asText()).isEqualTo("CREDIT");
        assertThat(t.get("entries").get(1).get("wallet_id").asText()).isEqualTo(bob);
        assertThat(r.header("Location")).isEqualTo("/api/v1/transactions/" + t.get("transaction_id").asText());
        assertThat(balance(alice)).isEqualTo(37_450);
        assertThat(balance(bob)).isEqualTo(12_550);
        assertThat(api().get("/api/v1/transactions/" + t.get("transaction_id").asText(), CLIENT).json().get("amount")
                .asLong()).isEqualTo(12_550);
    }

    @Test
    void sameWalletCurrencyMismatchAndInsufficientFundsAre422AndChangeNothing() {
        String a = newWallet("INR");
        String b = newWallet("INR");
        String usd = newWallet("USD");
        fund(a, 1_000, "INR");
        assertProblem(transfer(a, a, 10, "INR", newKey()), 422, "SAME_WALLET_TRANSFER");
        assertProblem(transfer(a, usd, 10, "INR", newKey()), 422, "CURRENCY_MISMATCH");
        assertProblem(transfer(a, b, 10, "USD", newKey()), 422, "CURRENCY_MISMATCH");
        assertProblem(transfer(a, b, 10, "EUR", newKey()), 422, "CURRENCY_MISMATCH");
        assertProblem(transfer(a, b, 1_001, "INR", newKey()), 422, "INSUFFICIENT_FUNDS");
        assertProblem(transfer(b, a, 1, "INR", newKey()), 422, "INSUFFICIENT_FUNDS");
        assertThat(balance(a)).isEqualTo(1_000);
        assertThat(balance(b)).isZero();
        assertThat(api().get("/api/v1/wallets/" + b + "/entries", CLIENT).json().get("total_count").asLong()).isZero();
        assertThat(transfer(a, b, 1_000, "INR", newKey()).status()).as("the whole balance may be sent").isEqualTo(201);
        assertThat(balance(a)).isZero();
    }

    @Test
    void invalidAmountsAre400() {
        String a = newWallet("INR");
        String b = newWallet("INR");
        fund(a, 1_000, "INR");
        for (String amount : new String[] {"0", "-5", "10.5", "1e3", "1E3", "100.0", "\"100\"", "true", "false",
            "null", "9007199254740992"}) {
            String json = "{\"source_wallet_id\":\"" + a + "\",\"destination_wallet_id\":\"" + b + "\",\"amount\":"
                    + amount + ",\"currency\":\"INR\"}";
            ApiClient.Response r = assertProblem(transferJson(json), 400, "VALIDATION_ERROR");
            assertThat(r.json().get("errors").get(0).get("field").asText()).as(amount).isEqualTo("amount");
        }
        assertThat(balance(a)).isEqualTo(1_000);
    }

    @Test
    void unknownSystemAndMalformedWallets() {
        String a = newWallet("INR");
        fund(a, 100, "INR");
        String system = transactions.inReadTransaction(() -> systemAccounts.fundingWallet("INR")).walletId().toString();
        assertProblem(transfer(a, UUID.randomUUID().toString(), 1, "INR", newKey()), 404, "WALLET_NOT_FOUND");
        assertProblem(transfer(UUID.randomUUID().toString(), a, 1, "INR", newKey()), 404, "WALLET_NOT_FOUND");
        assertProblem(transfer(a, system, 1, "INR", newKey()), 404, "WALLET_NOT_FOUND");
        assertProblem(transfer(system, a, 1, "INR", newKey()), 404, "WALLET_NOT_FOUND");
        assertProblem(transfer(a, "not-a-uuid", 1, "INR", newKey()), 400, "INVALID_ID");
        assertProblem(transfer("12345", a, 1, "INR", newKey()), 400, "INVALID_ID");
        assertThat(balance(a)).isEqualTo(100);
    }

    @Test
    void missingAndUnknownFieldsAre400() {
        String a = newWallet("INR");
        String b = newWallet("INR");
        assertProblem(transferJson("{\"destination_wallet_id\":\"" + b + "\",\"amount\":1,\"currency\":\"INR\"}"),
                400, "VALIDATION_ERROR");
        assertProblem(transferJson("{\"source_wallet_id\":\"" + a + "\",\"destination_wallet_id\":\"" + b
                + "\",\"amount\":1,\"currency\":\"INR\",\"fee\":0}"), 400, "VALIDATION_ERROR");
        assertProblem(transferJson("{\"source_wallet_id\":5,\"destination_wallet_id\":\"" + b
                + "\",\"amount\":1,\"currency\":\"INR\"}"), 400, "VALIDATION_ERROR");
        assertProblem(transferJson("{\"source_wallet_id\":\"" + a + "\",\"destination_wallet_id\":\"" + b
                + "\",\"amount\":1,\"currency\":\"INR\",\"description\":\"" + "x".repeat(256) + "\"}"), 400,
                "VALIDATION_ERROR");
    }

    @Test
    void headerChecksComeBeforeTheBody() {
        assertProblem(api().post("/api/v1/transfers", CLIENT, "{}"), 400, "IDEMPOTENCY_KEY_MISSING");
        assertProblem(api().post("/api/v1/transfers", CLIENT, "{}", "Idempotency-Key", "bad"), 400,
                "IDEMPOTENCY_KEY_INVALID");
        assertProblem(api().send("POST", "/api/v1/transfers", CLIENT, "text/plain", "{}".getBytes()), 415,
                "UNSUPPORTED_MEDIA_TYPE");
    }
}
