package edu.iiitb.team007.ledger.api;

import static org.assertj.core.api.Assertions.assertThat;

import edu.iiitb.team007.ledger.testsupport.ApiClient;
import edu.iiitb.team007.ledger.testsupport.IntegrationTestSupport;
import java.io.ByteArrayInputStream;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Behaviour common to every endpoint (FR-0.1-FR-0.5, SEC-4, UI-3, TQ-SEC): authentication, roles, the error
 * format, request IDs, body size and media type, in the order of SRS 4.7.2.
 */
class CrossCuttingApiTest extends IntegrationTestSupport {

    private static final String USER_BODY = "{\"name\":\"N\",\"email\":\"n@example.com\"}";

    @Test
    void missingOrUnknownKeyIs401() {
        assertProblem(api().get("/api/v1/users/" + UUID.randomUUID(), null), 401, "UNAUTHENTICATED");
        assertProblem(api().get("/api/v1/users/" + UUID.randomUUID(), "wrong-key-" + "x".repeat(40)), 401,
                "UNAUTHENTICATED");
        assertProblem(api().post("/api/v1/users", "", USER_BODY), 401, "UNAUTHENTICATED");
        assertProblem(api().get("/api/v1/users/" + UUID.randomUUID(), CLIENT.toUpperCase()), 401, "UNAUTHENTICATED");
    }

    @Test
    void authenticationComesBeforeValidation() {
        assertProblem(api().get("/api/v1/users/not-a-uuid", null), 401, "UNAUTHENTICATED");
        assertProblem(api().send("POST", "/api/v1/users", null, "text/plain", new byte[] {1}), 401,
                "UNAUTHENTICATED");
    }

    @Test
    void clientKeysCannotUseAdministrativeOperations() {
        String id = UUID.randomUUID().toString();
        assertProblem(api().get("/api/v1/admin/reconciliation", CLIENT), 403, "FORBIDDEN");
        assertProblem(api().post("/api/v1/wallets/" + id + "/deposits", CLIENT, "{}"), 403, "FORBIDDEN");
        assertProblem(api().post("/api/v1/transactions/" + id + "/reversal", CLIENT, "{}"), 403, "FORBIDDEN");
    }

    @Test
    void adminKeysMayCallClientEndpoints() {
        ApiClient.Response r = api().post("/api/v1/users", ADMIN,
                "{\"name\":\"Admin made\",\"email\":\"" + uniqueEmail() + "\"}");
        assertThat(r.status()).isEqualTo(201);
        assertThat(api().get("/api/v1/users/" + r.json().get("user_id").asText(), ADMIN).status()).isEqualTo(200);
    }

    @Test
    void healthNeedsNoKeyEvenWhenAWrongKeyIsSent() {
        assertThat(api().get("/health", null).status()).isEqualTo(200);
        assertThat(api().get("/health", "wrong").status()).isEqualTo(200);
    }

    @Test
    void requestIdIsEchoedOrGenerated() {
        ApiClient.Response echoed = api().get("/health", null, "X-Request-ID", "my-trace-123");
        assertThat(echoed.header("X-Request-ID")).isEqualTo("my-trace-123");

        ApiClient.Response generated = api().get("/health", null);
        assertThat(generated.header("X-Request-ID")).matches("[0-9a-f-]{36}");

        ApiClient.Response tooLong = api().get("/health", null, "X-Request-ID", "r".repeat(65));
        assertThat(tooLong.header("X-Request-ID")).matches("[0-9a-f-]{36}");

        ApiClient.Response onError = api().get("/api/v1/users/x", CLIENT, "X-Request-ID", "err-trace");
        assertThat(onError.header("X-Request-ID")).isEqualTo("err-trace");
        assertThat(onError.json().get("request_id").asText()).isEqualTo("err-trace");

        ApiClient.Response unauthenticated = api().get("/api/v1/users/x", null, "X-Request-ID", "auth-trace");
        assertThat(unauthenticated.json().get("request_id").asText()).isEqualTo("auth-trace");
    }

    @Test
    void bodiesOver16KibAre413WithoutProcessing() {
        String big = "{\"name\":\"" + "a".repeat(16 * 1024) + "\",\"email\":\"" + uniqueEmail() + "\"}";
        assertProblem(api().post("/api/v1/users", CLIENT, big), 413, "PAYLOAD_TOO_LARGE");

        HttpRequest chunked = HttpRequest.newBuilder(api().uri("/api/v1/users"))
                .header("X-API-Key", CLIENT).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofInputStream(
                        () -> new ByteArrayInputStream(big.getBytes(StandardCharsets.UTF_8)))).build();
        assertProblem(api().send(chunked), 413, "PAYLOAD_TOO_LARGE");

        String justFits = "{\"name\":\"ok\",\"email\":\"" + uniqueEmail() + "\"}";
        assertThat(api().post("/api/v1/users", CLIENT, justFits).status()).isEqualTo(201);
    }

    @Test
    void postWithoutJsonContentTypeIs415() {
        byte[] body = USER_BODY.getBytes(StandardCharsets.UTF_8);
        assertProblem(api().send("POST", "/api/v1/users", CLIENT, "text/plain", body), 415, "UNSUPPORTED_MEDIA_TYPE");
        assertProblem(api().send("POST", "/api/v1/users", CLIENT, null, body), 415, "UNSUPPORTED_MEDIA_TYPE");
        assertProblem(api().send("POST", "/api/v1/users", CLIENT, "application/json; charset=iso-8859-1", body),
                415, "UNSUPPORTED_MEDIA_TYPE");
        ApiClient.Response withCharset = api().send("POST", "/api/v1/users", CLIENT,
                "application/json; charset=utf-8",
                ("{\"name\":\"N\",\"email\":\"" + uniqueEmail() + "\"}").getBytes(StandardCharsets.UTF_8));
        assertThat(withCharset.status()).isEqualTo(201);
    }

    @Test
    void unknownPathsAndMethodsAreProblemDocumentsToo() {
        ApiClient.Response missing = api().get("/api/v1/nothing-here", CLIENT);
        assertThat(missing.status()).isEqualTo(404);
        assertThat(missing.header("Content-Type")).startsWith("application/problem+json");
        assertThat(missing.json().get("type").asText()).isEqualTo("about:blank");
        assertThat(missing.json().get("request_id").asText()).isEqualTo(missing.header("X-Request-ID"));

        ApiClient.Response method = api().send("DELETE", "/api/v1/users/" + UUID.randomUUID(), CLIENT, null, null);
        assertThat(method.status()).isEqualTo(405);
        assertThat(method.header("Content-Type")).startsWith("application/problem+json");
        assertThat(method.header("Allow")).contains("GET");
    }
}
