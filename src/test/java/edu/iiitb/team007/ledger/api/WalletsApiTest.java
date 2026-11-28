package edu.iiitb.team007.ledger.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import edu.iiitb.team007.ledger.service.SystemAccounts;
import edu.iiitb.team007.ledger.persistence.TransactionRunner;
import edu.iiitb.team007.ledger.testsupport.ApiClient;
import edu.iiitb.team007.ledger.testsupport.ApiTestSupport;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** System tests of wallets through HTTP (FR-1.3, FR-1.4, FR-1.5, FR-1.9, FR-6.1, FR-6.2, TQ-3, TQ-4). */
class WalletsApiTest extends ApiTestSupport {

    @Autowired
    private SystemAccounts systemAccounts;

    @Autowired
    private TransactionRunner transactions;

    private ApiClient.Response createWallet(String userId, String json) {
        return api().post("/api/v1/users/" + userId + "/wallets", CLIENT, json);
    }

    @Test
    void createsAWalletAtBalanceZeroWithLocation() {
        String user = newUser();
        ApiClient.Response r = createWallet(user, "{\"currency\":\"INR\"}");

        assertThat(r.status()).isEqualTo(201);
        JsonNode w = r.json();
        assertThat(w.get("user_id").asText()).isEqualTo(user);
        assertThat(w.get("currency").asText()).isEqualTo("INR");
        assertThat(w.get("balance").isIntegralNumber()).isTrue();
        assertThat(w.get("balance").asLong()).isZero();
        assertThat(w.get("created_at").asText()).endsWith("Z");
        assertThat(r.header("Location")).isEqualTo("/api/v1/wallets/" + w.get("wallet_id").asText());
        assertThat(api().get("/api/v1/wallets/" + w.get("wallet_id").asText(), CLIENT).json()).isEqualTo(w);
    }

    @Test
    void oneWalletPerCurrencyButSeveralCurrenciesPerUser() {
        String user = newUser();
        assertThat(createWallet(user, "{\"currency\":\"INR\"}").status()).isEqualTo(201);
        assertThat(createWallet(user, "{\"currency\":\"USD\"}").status()).isEqualTo(201);
        assertProblem(createWallet(user, "{\"currency\":\"INR\"}"), 409, "WALLET_ALREADY_EXISTS");

        JsonNode list = api().get("/api/v1/users/" + user + "/wallets", CLIENT).json();
        assertThat(list.get("user_id").asText()).isEqualTo(user);
        assertThat(list.get("wallets")).hasSize(2);
        assertThat(list.get("wallets").get(0).get("currency").asText()).isEqualTo("INR");
        assertThat(list.get("wallets").get(1).get("currency").asText()).isEqualTo("USD");
    }

    @Test
    void currencyFormatAndSupport() {
        String user = newUser();
        assertProblem(createWallet(user, "{\"currency\":\"EUR\"}"), 422, "UNSUPPORTED_CURRENCY");
        assertProblem(createWallet(user, "{\"currency\":\"ABC\"}"), 422, "UNSUPPORTED_CURRENCY");
        for (String bad : new String[] {"\"inr\"", "\"INRR\"", "\"IN\"", "\"\"", "null", "356", "true"}) {
            assertProblem(createWallet(user, "{\"currency\":" + bad + "}"), 400, "VALIDATION_ERROR");
        }
        assertProblem(createWallet(user, "{}"), 400, "VALIDATION_ERROR");
        assertProblem(createWallet(user, "{\"currency\":\"INR\",\"balance\":100}"), 400, "VALIDATION_ERROR");
    }

    @Test
    void unknownOrMalformedUser() {
        assertProblem(createWallet(UUID.randomUUID().toString(), "{\"currency\":\"INR\"}"), 404, "USER_NOT_FOUND");
        assertProblem(createWallet("nope", "{\"currency\":\"INR\"}"), 400, "INVALID_ID");
        assertProblem(api().get("/api/v1/users/" + UUID.randomUUID() + "/wallets", CLIENT), 404, "USER_NOT_FOUND");
        assertProblem(api().get("/api/v1/users/x/wallets", CLIENT), 400, "INVALID_ID");
    }

    @Test
    void unknownOrMalformedWallet() {
        assertProblem(api().get("/api/v1/wallets/" + UUID.randomUUID(), CLIENT), 404, "WALLET_NOT_FOUND");
        assertProblem(api().get("/api/v1/wallets/1234", CLIENT), 400, "INVALID_ID");
        assertProblem(api().get("/api/v1/wallets/" + UUID.randomUUID() + "/entries", CLIENT), 404, "WALLET_NOT_FOUND");
        assertProblem(api().get("/api/v1/wallets/zz/entries", CLIENT), 400, "INVALID_ID");
    }

    @Test
    void systemFundingAccountsAreNeverVisibleToClients() {
        String systemWallet = transactions.inReadTransaction(() -> systemAccounts.fundingWallet("INR"))
                .walletId().toString();
        String systemUser = transactions.inReadTransaction(() -> systemAccounts.fundingWallet("INR"))
                .userId().toString();
        assertProblem(api().get("/api/v1/wallets/" + systemWallet, CLIENT), 404, "WALLET_NOT_FOUND");
        assertProblem(api().get("/api/v1/wallets/" + systemWallet + "/entries", CLIENT), 404, "WALLET_NOT_FOUND");
        assertProblem(api().get("/api/v1/users/" + systemUser, CLIENT), 404, "USER_NOT_FOUND");
        assertProblem(api().get("/api/v1/users/" + systemUser + "/wallets", CLIENT), 404, "USER_NOT_FOUND");
        assertProblem(createWallet(systemUser, "{\"currency\":\"USD\"}"), 404, "USER_NOT_FOUND");
    }

    @Test
    void entriesOfANewWalletAreAnEmptyPage() {
        String wallet = newWallet("INR");
        JsonNode page = api().get("/api/v1/wallets/" + wallet + "/entries", CLIENT).json();
        assertThat(page.get("wallet_id").asText()).isEqualTo(wallet);
        assertThat(page.get("entries")).isEmpty();
        assertThat(page.get("total_count").asLong()).isZero();
        assertThat(page.get("limit").asInt()).isEqualTo(50);
        assertThat(page.get("offset").asInt()).isZero();
    }

    @Test
    void invalidPagingIs400() {
        String wallet = newWallet("INR");
        for (String q : new String[] {"limit=0", "limit=101", "limit=-1", "limit=abc", "offset=-1", "offset=1.5"}) {
            ApiClient.Response r = assertProblem(api().get("/api/v1/wallets/" + wallet + "/entries?" + q, CLIENT),
                    400, "VALIDATION_ERROR");
            assertThat(r.json().get("errors").get(0).get("field").asText()).isEqualTo(q.substring(0, q.indexOf('=')));
        }
    }
}
