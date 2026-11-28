package edu.iiitb.team007.ledger.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import edu.iiitb.team007.ledger.persistence.DatabaseHealth;
import edu.iiitb.team007.ledger.testsupport.ApiClient;
import edu.iiitb.team007.ledger.testsupport.TestDatabase;
import edu.iiitb.team007.ledger.testsupport.TestKeys;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** GET /health answers 503 "degraded" when the database is unusable - the provider is substituted (FR-6.4, SE-6). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HealthControllerDegradedTest {

    @DynamicPropertySource
    static void isolatedDatabase(DynamicPropertyRegistry registry) {
        registry.add("ledger.db-path", () -> TestDatabase.fresh("degraded").toString());
        TestKeys.register(registry);
    }

    @LocalServerPort
    private int port;

    @MockitoBean
    private DatabaseHealth databaseHealth;

    @Test
    void healthIsDegradedWhenDatabaseQueryFails() {
        when(databaseHealth.isAvailable()).thenReturn(false);

        ApiClient.Response r = new ApiClient(port).get("/health", null);

        assertThat(r.status()).isEqualTo(503);
        assertThat(r.json().get("status").asText()).isEqualTo("degraded");
        assertThat(r.json().get("database").asText()).isEqualTo("unavailable");
    }
}
