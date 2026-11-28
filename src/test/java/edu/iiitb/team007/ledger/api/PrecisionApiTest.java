package edu.iiitb.team007.ledger.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import edu.iiitb.team007.ledger.testsupport.ApiClient;
import edu.iiitb.team007.ledger.testsupport.IntegrationTestSupport;
import org.junit.jupiter.api.Test;

/**
 * Floating-point rounding cannot occur (TQ-6, CON-3, CI-2, NFR-REL-3, SEC-2): integer minor units end to end,
 * fractional, exponent, string and boolean amounts rejected, and many small transfers sum exactly.
 */
class PrecisionApiTest extends IntegrationTestSupport {

    @Test
    void nonIntegerAmountsAreRejectedEverywhere() {
        String a = newWallet("INR");
        String b = newWallet("INR");
        fund(a, 1_000, "INR");
        for (String amount : new String[] {"10.5", "1e3", "\"100\"", "true", "0.1", "-0.0", "1.0E2"}) {
            ApiClient.Response deposit = api().post("/api/v1/wallets/" + a + "/deposits", ADMIN,
                    "{\"amount\":" + amount + ",\"currency\":\"INR\"}", "Idempotency-Key", newKey());
            assertProblem(deposit, 400, "VALIDATION_ERROR");
            ApiClient.Response transfer = api().post("/api/v1/transfers", CLIENT, "{\"source_wallet_id\":\"" + a
                    + "\",\"destination_wallet_id\":\"" + b + "\",\"amount\":" + amount + ",\"currency\":\"INR\"}",
                    "Idempotency-Key", newKey());
            assertProblem(transfer, 400, "VALIDATION_ERROR");
        }
        assertThat(balance(a)).isEqualTo(1_000);
        assertThat(balance(b)).isZero();
    }

    @Test
    void thousandTransfersOfOneMinorUnitAreExact() {
        String a = newWallet("USD");
        String b = newWallet("USD");
        fund(a, 1_000, "USD");
        for (int i = 0; i < 1_000; i++) {
            assertThat(transfer(a, b, 1, "USD", newKey()).status()).isEqualTo(201);
        }
        assertThat(balance(a)).isZero();
        assertThat(balance(b)).isEqualTo(1_000);
        JsonNode page = api().get("/api/v1/wallets/" + b + "/entries?limit=100&offset=900", CLIENT).json();
        assertThat(page.get("total_count").asLong()).isEqualTo(1_000);
        assertThat(page.get("entries")).hasSize(100);
    }

    @Test
    void amountsAndBalancesAreSentAsJsonIntegers() {
        String a = newWallet("INR");
        ApiClient.Response r = deposit(a, 10_050, "INR", newKey());
        assertThat(r.json().get("amount").isIntegralNumber()).isTrue();
        assertThat(r.body()).contains("\"amount\":10050").doesNotContain("10050.0").doesNotContain("\"10050\"");
        JsonNode wallet = api().get("/api/v1/wallets/" + a, CLIENT).json();
        assertThat(wallet.get("balance").isIntegralNumber()).isTrue();
        assertThat(wallet.get("balance").asLong()).isEqualTo(10_050);
    }
}
