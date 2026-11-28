package edu.iiitb.team007.ledger.api;

import static org.assertj.core.api.Assertions.assertThat;

import edu.iiitb.team007.ledger.testsupport.ApiClient;
import edu.iiitb.team007.ledger.testsupport.IntegrationTestSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * Race safety under simultaneous requests through HTTP (FR-3.1-FR-3.5, FR-2.8, NFR-PERF-3, TQ-4): requests are
 * released at the same instant by a latch; the database write lock serialises them.
 */
class ConcurrencyApiTest extends IntegrationTestSupport {

    /** Result of one concurrent request: status, error code and how long it took. */
    private record Outcome(int status, String code, long millis, String body, String replayed) {
    }

    private List<Outcome> concurrently(List<Callable<ApiClient.Response>> calls) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(calls.size());
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Outcome>> futures = new ArrayList<>();
            for (Callable<ApiClient.Response> call : calls) {
                futures.add(pool.submit(() -> {
                    start.await();
                    long t0 = System.nanoTime();
                    ApiClient.Response r = call.call();
                    long ms = (System.nanoTime() - t0) / 1_000_000;
                    String code = r.status() >= 400 ? r.json().get("code").asText() : null;
                    return new Outcome(r.status(), code, ms, r.body(), r.header("Idempotent-Replayed"));
                }));
            }
            start.countDown();
            List<Outcome> outcomes = new ArrayList<>();
            for (Future<Outcome> f : futures) {
                outcomes.add(f.get(30, TimeUnit.SECONDS));
            }
            return outcomes;
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void twentyConcurrentTransfersOfHundredFromThousandExactlyTenSucceed() throws Exception {
        String source = newWallet("INR");
        fund(source, 1_000, "INR");
        List<String> destinations = List.of(newWallet("INR"), newWallet("INR"), newWallet("INR"), newWallet("INR"));
        List<Callable<ApiClient.Response>> calls = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            String to = destinations.get(i % destinations.size());
            calls.add(() -> transfer(source, to, 100, "INR", newKey()));
        }
        List<Outcome> outcomes = concurrently(calls);

        assertThat(outcomes).filteredOn(o -> o.status() == 201).hasSize(10);
        assertThat(outcomes).filteredOn(o -> o.status() == 422 && "INSUFFICIENT_FUNDS".equals(o.code())).hasSize(10);
        assertThat(outcomes).as("NFR-PERF-3: no 5xx").noneMatch(o -> o.status() >= 500);
        assertThat(outcomes).as("NFR-PERF-3: each within 10 s").allMatch(o -> o.millis() < 10_000);
        assertThat(balance(source)).isZero();
        assertThat(destinations.stream().mapToLong(this::balance).sum()).isEqualTo(1_000);
    }

    @Test
    void twoConcurrentTransfersOfSixHundredFromThousandExactlyOneSucceeds() throws Exception {
        String source = newWallet("INR");
        String d1 = newWallet("INR");
        String d2 = newWallet("INR");
        fund(source, 1_000, "INR");
        List<Outcome> outcomes = concurrently(List.of(() -> transfer(source, d1, 600, "INR", newKey()),
                () -> transfer(source, d2, 600, "INR", newKey())));

        assertThat(outcomes).filteredOn(o -> o.status() == 201).hasSize(1);
        assertThat(outcomes).filteredOn(o -> "INSUFFICIENT_FUNDS".equals(o.code())).hasSize(1);
        assertThat(balance(source)).isEqualTo(400);
        assertThat(balance(d1) + balance(d2)).isEqualTo(600);
    }

    @Test
    void concurrentRequestsWithTheSameKeyPostAtMostOnce() throws Exception {
        String source = newWallet("INR");
        String destination = newWallet("INR");
        fund(source, 1_000, "INR");
        String key = newKey();
        List<Callable<ApiClient.Response>> calls = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            calls.add(() -> transfer(source, destination, 250, "INR", key));
        }
        List<Outcome> outcomes = concurrently(calls);

        assertThat(outcomes).allMatch(o -> o.status() == 201);
        assertThat(outcomes).filteredOn(o -> o.replayed() == null).hasSize(1);
        assertThat(outcomes).filteredOn(o -> "true".equals(o.replayed())).hasSize(9);
        assertThat(outcomes.stream().map(Outcome::body).distinct()).hasSize(1);
        assertThat(balance(source)).isEqualTo(750);
        assertThat(balance(destination)).isEqualTo(250);
    }

    @Test
    void simultaneousCreditsAndDebitsOnOneWalletLoseNoUpdate() throws Exception {
        String hub = newWallet("INR");
        String other = newWallet("INR");
        fund(hub, 500, "INR");
        fund(other, 500, "INR");
        List<Callable<ApiClient.Response>> calls = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            calls.add(() -> transfer(hub, other, 30, "INR", newKey()));
            calls.add(() -> transfer(other, hub, 20, "INR", newKey()));
        }
        List<Outcome> outcomes = concurrently(calls);

        assertThat(outcomes).allMatch(o -> o.status() == 201);
        assertThat(balance(hub)).isEqualTo(500 - 10 * 30 + 10 * 20);
        assertThat(balance(other)).isEqualTo(500 + 10 * 30 - 10 * 20);
    }
}
