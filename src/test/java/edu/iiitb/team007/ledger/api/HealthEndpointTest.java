package edu.iiitb.team007.ledger.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** System test of GET /health through real HTTP, against a fresh database file (FR-6.4, TQ-3, TQ-5). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HealthEndpointTest {

    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    static void isolatedDatabase(DynamicPropertyRegistry registry) {
        registry.add("ledger.db-path", () -> tempDir.resolve("test-ledger.db").toString());
    }

    @Autowired
    private TestRestTemplate rest;

    @Test
    void healthIsOkWithVersionWhenDatabaseIsUsable() {
        ResponseEntity<Map<String, String>> response = rest.exchange(
                "/health", HttpMethod.GET, null, new ParameterizedTypeReference<Map<String, String>>() { });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("status", "ok").containsEntry("database", "ok");
        assertThat(response.getBody().get("version")).isNotBlank().doesNotContain("@");
    }

    @Test
    void healthNeedsNoApiKeyAndSendsJson() {
        ResponseEntity<String> response = rest.getForEntity("/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isNotNull();
        assertThat(response.getHeaders().getContentType().toString()).startsWith("application/json");
    }
}
