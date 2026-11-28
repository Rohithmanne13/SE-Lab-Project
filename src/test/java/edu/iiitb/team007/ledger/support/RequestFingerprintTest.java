package edu.iiitb.team007.ledger.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/** Idempotency-key fingerprint hashing (FR-2.4, TQ-1). */
class RequestFingerprintTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void fingerprintMatchesIndependentlyComputedVector() {
        String body = "{\"amount\":100,\"currency\":\"INR\",\"destination_wallet_id\":\"b\","
                + "\"source_wallet_id\":\"a\"}";
        assertThat(RequestFingerprint.of("POST", "/api/v1/transfers", body))
                .isEqualTo("8b424363dd7fe931f968d8efffc3eeafa168781bae32dc77df50b4a9a67319bc");
    }

    @Test
    void keyOrderAndWhitespaceDoNotChangeTheFingerprint() throws Exception {
        String a = CanonicalJson.of(mapper.readTree("{\"source_wallet_id\":\"a\", \"amount\": 100,\n"
                + " \"currency\":\"INR\",\"destination_wallet_id\":\"b\"}"));
        String b = CanonicalJson.of(mapper.readTree("{\"amount\":100,\"currency\":\"INR\","
                + "\"destination_wallet_id\":\"b\",\"source_wallet_id\":\"a\"}"));
        assertThat(RequestFingerprint.of("POST", "/p", a)).isEqualTo(RequestFingerprint.of("POST", "/p", b));
    }

    @Test
    void methodPathAndBodyEachChangeTheFingerprint() {
        String base = RequestFingerprint.of("POST", "/api/v1/transfers", "{\"amount\":100}");
        assertThat(RequestFingerprint.of("PUT", "/api/v1/transfers", "{\"amount\":100}")).isNotEqualTo(base);
        assertThat(RequestFingerprint.of("POST", "/api/v1/transfer", "{\"amount\":100}")).isNotEqualTo(base);
        assertThat(RequestFingerprint.of("POST", "/api/v1/transfers", "{\"amount\":101}")).isNotEqualTo(base);
    }
}
