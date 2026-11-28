package edu.iiitb.team007.ledger.api;

import static org.assertj.core.api.Assertions.assertThat;

import edu.iiitb.team007.ledger.testsupport.ApiClient;
import edu.iiitb.team007.ledger.testsupport.ApiTestSupport;
import org.junit.jupiter.api.Test;

/** System test of GET /health through real HTTP, against the isolated test database (FR-6.4, TQ-3, TQ-5). */
class HealthEndpointTest extends ApiTestSupport {

    @Test
    void healthIsOkWithVersionWhenDatabaseIsUsable() {
        ApiClient.Response r = api().get("/health", null);

        assertThat(r.status()).isEqualTo(200);
        assertThat(r.json().get("status").asText()).isEqualTo("ok");
        assertThat(r.json().get("database").asText()).isEqualTo("ok");
        assertThat(r.json().get("version").asText()).isNotBlank().doesNotContain("@");
    }

    @Test
    void healthNeedsNoApiKeyAndSendsJson() {
        ApiClient.Response r = api().get("/health", null);

        assertThat(r.status()).isEqualTo(200);
        assertThat(r.header("Content-Type")).startsWith("application/json");
        assertThat(r.header("X-Request-ID")).isNotBlank();
    }
}
