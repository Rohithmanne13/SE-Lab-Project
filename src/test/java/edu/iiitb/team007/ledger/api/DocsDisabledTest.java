package edu.iiitb.team007.ledger.api;

import static org.assertj.core.api.Assertions.assertThat;

import edu.iiitb.team007.ledger.testsupport.ApiClient;
import edu.iiitb.team007.ledger.testsupport.ApiTestSupport;
import edu.iiitb.team007.ledger.testsupport.TestDatabase;
import edu.iiitb.team007.ledger.testsupport.TestKeys;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Outside development mode the documentation pages can be switched off (UI-1: "in development mode"). */
class DocsDisabledTest extends ApiTestSupport {

    @DynamicPropertySource
    static void docsOff(DynamicPropertyRegistry registry) {
        registry.add("ledger.db-path", () -> TestDatabase.fresh("docs-off").toString());
        registry.add("ledger.docs-enabled", () -> "false");
        TestKeys.register(registry);
    }

    @Test
    void docsAndOpenApiFileAreNotServed() {
        for (String path : new String[] {"/docs", "/openapi.yaml"}) {
            ApiClient.Response r = api().get(path, null);
            assertThat(r.status()).isEqualTo(404);
            assertThat(r.header("Content-Type")).startsWith("application/problem+json");
        }
        assertThat(api().get("/health", null).status()).isEqualTo(200);
    }
}
