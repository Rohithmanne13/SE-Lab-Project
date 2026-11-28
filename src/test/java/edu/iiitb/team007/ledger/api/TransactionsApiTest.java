package edu.iiitb.team007.ledger.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import edu.iiitb.team007.ledger.testsupport.ApiClient;
import edu.iiitb.team007.ledger.testsupport.IntegrationTestSupport;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Transaction and ledger-history queries (FR-6.2, FR-6.3, FR-4.1, FR-4.3, TQ-3). */
class TransactionsApiTest extends IntegrationTestSupport {

    @Test
    void getTransactionReturnsTypeAmountCurrencyTimestampsDescriptionAndEntries() {
        String a = newWallet("INR");
        String b = newWallet("INR");
        fund(a, 900, "INR");
        ApiClient.Response created = api().post("/api/v1/transfers", CLIENT, "{\"source_wallet_id\":\"" + a
                + "\",\"destination_wallet_id\":\"" + b + "\",\"amount\":250,\"currency\":\"INR\","
                + "\"description\":\"rent\"}",
                "Idempotency-Key", newKey());
        String id = created.json().get("transaction_id").asText();

        ApiClient.Response r = api().get("/api/v1/transactions/" + id, CLIENT);
        assertThat(r.status()).isEqualTo(200);
        JsonNode t = r.json();
        assertThat(t).isEqualTo(created.json());
        assertThat(t.get("reversed_by_transaction_id").isNull()).isTrue();
        assertThat(t.get("reverses_transaction_id").isNull()).isTrue();
        for (JsonNode e : t.get("entries")) {
            assertThat(e.get("transaction_id").asText()).isEqualTo(id);
            assertThat(e.get("entry_hash").asText()).matches("[0-9a-f]{64}");
            assertThat(e.get("prev_hash").asText()).matches("[0-9a-f]{64}");
            assertThat(e.get("amount").asLong()).isPositive();
        }
    }

    @Test
    void unknownAndMalformedTransactionIds() {
        assertProblem(api().get("/api/v1/transactions/" + UUID.randomUUID(), CLIENT), 404, "TRANSACTION_NOT_FOUND");
        assertProblem(api().get("/api/v1/transactions/abc", CLIENT), 400, "INVALID_ID");
    }

    @Test
    void walletEntriesArePagedInAscendingSequenceWithTheTotalCount() {
        String a = newWallet("INR");
        String b = newWallet("INR");
        fund(a, 1_000, "INR");
        for (int i = 1; i <= 5; i++) {
            assertThat(transfer(a, b, i, "INR", newKey()).status()).isEqualTo(201);
        }
        JsonNode all = api().get("/api/v1/wallets/" + a + "/entries", CLIENT).json();
        assertThat(all.get("total_count").asLong()).isEqualTo(6);
        assertThat(all.get("entries")).hasSize(6);
        long previous = 0;
        for (JsonNode e : all.get("entries")) {
            assertThat(e.get("sequence").asLong()).isGreaterThan(previous);
            assertThat(e.get("wallet_id").asText()).isEqualTo(a);
            previous = e.get("sequence").asLong();
        }
        assertThat(all.get("entries").get(0).get("direction").asText()).isEqualTo("CREDIT");

        JsonNode page = api().get("/api/v1/wallets/" + a + "/entries?limit=2&offset=1", CLIENT).json();
        assertThat(page.get("limit").asInt()).isEqualTo(2);
        assertThat(page.get("offset").asInt()).isEqualTo(1);
        assertThat(page.get("total_count").asLong()).isEqualTo(6);
        assertThat(page.get("entries")).hasSize(2);
        assertThat(page.get("entries").get(0)).isEqualTo(all.get("entries").get(1));
        assertThat(page.get("entries").get(1)).isEqualTo(all.get("entries").get(2));

        JsonNode beyond = api().get("/api/v1/wallets/" + a + "/entries?offset=100", CLIENT).json();
        assertThat(beyond.get("entries")).isEmpty();
        assertThat(beyond.get("total_count").asLong()).isEqualTo(6);
    }

    @Test
    void noOperationUpdatesOrDeletesTransactions() {
        String a = newWallet("INR");
        fund(a, 10, "INR");
        String id = api().get("/api/v1/wallets/" + a + "/entries", CLIENT).json().get("entries").get(0)
                .get("transaction_id").asText();
        for (String method : new String[] {"PUT", "PATCH", "DELETE"}) {
            ApiClient.Response r = api().send(method, "/api/v1/transactions/" + id, ADMIN, "application/json",
                    "{}".getBytes());
            assertThat(r.status()).as(method).isEqualTo(405);
        }
        assertThat(api().get("/api/v1/transactions/" + id, CLIENT).status()).isEqualTo(200);
        assertThat(balance(a)).isEqualTo(10);
    }
}
