package edu.iiitb.team007.ledger.api;

import static org.assertj.core.api.Assertions.assertThat;

import edu.iiitb.team007.ledger.testsupport.ApiClient;
import edu.iiitb.team007.ledger.testsupport.IntegrationTestSupport;
import java.sql.Connection;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** A write lock not obtained within the 5000 ms busy timeout gives 503 SERVICE_BUSY, nothing persisted (FR-3.6). */
class BusyApiTest extends IntegrationTestSupport {

    @Autowired
    private DataSource dataSource;

    @Test
    void transferWhileAnotherWriterHoldsTheLockIs503WithRetryAfter() throws Exception {
        String a = newWallet("INR");
        String b = newWallet("INR");
        fund(a, 1_000, "INR");
        String key = newKey();
        ApiClient.Response busy;
        try (Connection other = dataSource.getConnection(); Statement s = other.createStatement()) {
            s.execute("BEGIN IMMEDIATE");
            long t0 = System.nanoTime();
            busy = transfer(a, b, 100, "INR", key);
            assertThat((System.nanoTime() - t0) / 1_000_000).isGreaterThanOrEqualTo(4_900);
            s.execute("ROLLBACK");
        }
        assertProblem(busy, 503, "SERVICE_BUSY");
        assertThat(busy.header("Retry-After")).isEqualTo("1");
        assertThat(balance(a)).isEqualTo(1_000);

        ApiClient.Response retry = transfer(a, b, 100, "INR", key);
        assertThat(retry.status()).as("nothing was stored for the key").isEqualTo(201);
        assertThat(retry.header("Idempotent-Replayed")).isNull();
        assertThat(balance(a)).isEqualTo(900);
    }
}
