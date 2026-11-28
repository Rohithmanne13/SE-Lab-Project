package edu.iiitb.team007.ledger.testsupport;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * The real service on a random port with helpers for system tests through the HTTP API (TQ-3). It does not choose
 * a database: extend {@link IntegrationTestSupport} for the shared test database, or declare an own
 * {@code @DynamicPropertySource} with {@link TestDatabase#fresh} and {@link TestKeys#register} (TQ-5).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class ApiTestSupport {

    protected static final String CLIENT = TestKeys.CLIENT;
    protected static final String ADMIN = TestKeys.ADMIN;

    @LocalServerPort
    protected int port;

    protected ApiClient api() {
        return new ApiClient(port);
    }

    protected static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    /** Creates a user and returns its user_id. */
    protected String newUser() {
        ApiClient.Response r = api().post("/api/v1/users", CLIENT,
                "{\"name\":\"Test User\",\"email\":\"" + uniqueEmail() + "\"}");
        assertThat(r.status()).as(r.body()).isEqualTo(201);
        return r.json().get("user_id").asText();
    }

    /** Creates a wallet in the currency for a new user and returns its wallet_id. */
    protected String newWallet(String currency) {
        return newWallet(newUser(), currency);
    }

    protected String newWallet(String userId, String currency) {
        ApiClient.Response r = api().post("/api/v1/users/" + userId + "/wallets", CLIENT,
                "{\"currency\":\"" + currency + "\"}");
        assertThat(r.status()).as(r.body()).isEqualTo(201);
        return r.json().get("wallet_id").asText();
    }

    /** A new random Idempotency-Key (UUID v4, as SRS 3.3.3 recommends). */
    protected static String newKey() {
        return UUID.randomUUID().toString();
    }

    /** ADMIN deposit into a wallet; returns the response. */
    protected ApiClient.Response deposit(String walletId, long amount, String currency, String key) {
        return api().post("/api/v1/wallets/" + walletId + "/deposits", ADMIN,
                "{\"amount\":" + amount + ",\"currency\":\"" + currency + "\"}", "Idempotency-Key", key);
    }

    /** Deposits and asserts 201. */
    protected void fund(String walletId, long amount, String currency) {
        ApiClient.Response r = deposit(walletId, amount, currency, newKey());
        assertThat(r.status()).as(r.body()).isEqualTo(201);
    }

    /** CLIENT transfer; returns the response. */
    protected ApiClient.Response transfer(String from, String to, long amount, String currency, String key) {
        return api().post("/api/v1/transfers", CLIENT, transferBody(from, to, amount, currency),
                "Idempotency-Key", key);
    }

    protected static String transferBody(String from, String to, long amount, String currency) {
        return "{\"source_wallet_id\":\"" + from + "\",\"destination_wallet_id\":\"" + to + "\",\"amount\":"
                + amount + ",\"currency\":\"" + currency + "\"}";
    }

    /** Current balance of a wallet through GET /wallets/{walletId}. */
    protected long balance(String walletId) {
        ApiClient.Response r = api().get("/api/v1/wallets/" + walletId, CLIENT);
        assertThat(r.status()).as(r.body()).isEqualTo(200);
        return r.json().get("balance").asLong();
    }

    /** Asserts an RFC 9457 problem document with the code, and returns it (SRS 3.3.4, UI-3, SEC-6). */
    protected static ApiClient.Response assertProblem(ApiClient.Response r, int status, String code) {
        assertThat(r.status()).as(r.body()).isEqualTo(status);
        assertThat(r.header("Content-Type")).startsWith("application/problem+json");
        var json = r.json();
        assertThat(json.get("code").asText()).isEqualTo(code);
        assertThat(json.get("status").asInt()).isEqualTo(status);
        assertThat(json.get("type").asText()).endsWith("#" + code.toLowerCase());
        assertThat(json.get("title").asText()).isNotBlank();
        assertThat(json.get("detail").asText()).isNotBlank();
        assertThat(json.get("request_id").asText()).isEqualTo(r.header("X-Request-ID"));
        assertThat(r.body()).doesNotContain("Exception", "at edu.", "SQL", ".java", "/tmp/", "target/");
        return r;
    }
}
