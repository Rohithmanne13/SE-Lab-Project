package edu.iiitb.team007.ledger.testsupport;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.springframework.boot.test.web.server.LocalServerPort;

/** System tests through the HTTP API (TQ-3), with helpers to create users and wallets. */
public abstract class ApiTestSupport extends IntegrationTestSupport {

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
