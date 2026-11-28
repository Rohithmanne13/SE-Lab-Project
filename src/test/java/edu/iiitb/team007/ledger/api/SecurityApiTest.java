package edu.iiitb.team007.ledger.api;

import static org.assertj.core.api.Assertions.assertThat;

import edu.iiitb.team007.ledger.testsupport.ApiClient;
import edu.iiitb.team007.ledger.testsupport.IntegrationTestSupport;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Security tests (TQ-SEC, SEC-1, SEC-2, SEC-3, SEC-4, SEC-6, FR-0.1): authentication and authorisation on every
 * endpoint, negative-amount prevention, injection attempts and error bodies without internals.
 */
class SecurityApiTest extends IntegrationTestSupport {

    private record Endpoint(String method, String path, boolean admin) {
    }

    private static List<Endpoint> endpoints() {
        String id = UUID.randomUUID().toString();
        return List.of(new Endpoint("POST", "/api/v1/users", false),
                new Endpoint("GET", "/api/v1/users/" + id, false),
                new Endpoint("POST", "/api/v1/users/" + id + "/wallets", false),
                new Endpoint("GET", "/api/v1/users/" + id + "/wallets", false),
                new Endpoint("GET", "/api/v1/wallets/" + id, false),
                new Endpoint("GET", "/api/v1/wallets/" + id + "/entries", false),
                new Endpoint("POST", "/api/v1/wallets/" + id + "/deposits", true),
                new Endpoint("POST", "/api/v1/transfers", false),
                new Endpoint("GET", "/api/v1/transactions/" + id, false),
                new Endpoint("POST", "/api/v1/transactions/" + id + "/reversal", true),
                new Endpoint("GET", "/api/v1/admin/reconciliation", true));
    }

    private ApiClient.Response call(Endpoint e, String key) {
        byte[] body = e.method().equals("POST") ? "{}".getBytes(StandardCharsets.UTF_8) : null;
        return api().send(e.method(), e.path(), key, body == null ? null : "application/json", body,
                "Idempotency-Key", UUID.randomUUID().toString());
    }

    @Test
    void everyEndpointExceptHealthNeedsAValidKey() {
        for (Endpoint e : endpoints()) {
            assertProblem(call(e, null), 401, "UNAUTHENTICATED");
            assertProblem(call(e, "x".repeat(48)), 401, "UNAUTHENTICATED");
        }
        assertThat(api().get("/health", null).status()).isEqualTo(200);
    }

    @Test
    void clientKeysGet403OnAdminEndpointsOnly() {
        for (Endpoint e : endpoints()) {
            ApiClient.Response r = call(e, CLIENT);
            if (e.admin()) {
                assertProblem(r, 403, "FORBIDDEN");
            } else {
                assertThat(r.status()).as(e.toString()).isNotIn(401, 403);
            }
            assertThat(call(e, ADMIN).status()).as(e.toString()).isNotIn(401, 403);
        }
    }

    @Test
    void negativeAmountsCannotInflateABalance() {
        String attacker = newWallet("INR");
        String victim = newWallet("INR");
        fund(victim, 1_000, "INR");
        assertProblem(transfer(victim, attacker, -500, "INR", newKey()), 400, "VALIDATION_ERROR");
        assertProblem(transfer(attacker, victim, -500, "INR", newKey()), 400, "VALIDATION_ERROR");
        assertProblem(deposit(attacker, -500, "INR", newKey()), 400, "VALIDATION_ERROR");
        assertThat(balance(attacker)).isZero();
        assertThat(balance(victim)).isEqualTo(1_000);
    }

    @Test
    void injectionAttemptsInIdentifiersAreRejectedAsMalformed() {
        for (String id : new String[] {"1%27%20OR%20%271%27=%271", "x%3BDROP%20TABLE%20wallets", "..%2F..%2Fetc"}) {
            ApiClient.Response r = api().get("/api/v1/wallets/" + id, CLIENT);
            assertThat(r.status()).isIn(400, 404);
            assertThat(r.body()).doesNotContain("SQL", "Exception");
        }
        assertProblem(api().get("/api/v1/wallets/1'%20OR%20'1'='1", CLIENT), 400, "INVALID_ID");
        assertThat(reconcile().get("wallets_checked").asLong()).isPositive();
    }

    @Test
    void keysAreNeverReturned() {
        ApiClient.Response r = api().get("/api/v1/users/" + UUID.randomUUID(), CLIENT);
        assertThat(r.body()).doesNotContain(CLIENT);
        assertThat(r.headers().map().toString()).doesNotContain(CLIENT);
    }
}
