package edu.iiitb.team007.ledger.testsupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/** A minimal HTTP/1.1 client for system tests through the real API (TQ-3): raw bodies, any headers. */
public final class ApiClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();

    private final String base;

    public ApiClient(int port) {
        this.base = "http://127.0.0.1:" + port;
    }

    /** A response: status, headers and the body as text. */
    public record Response(int status, HttpHeaders headers, String body) {

        public JsonNode json() {
            try {
                return MAPPER.readTree(body);
            } catch (IOException e) {
                throw new UncheckedIOException("Response body is not JSON: " + body, e);
            }
        }

        public String header(String name) {
            return headers.firstValue(name).orElse(null);
        }
    }

    public Response get(String path, String key, String... headers) {
        return send("GET", path, key, null, null, headers);
    }

    public Response post(String path, String key, String json, String... headers) {
        return send("POST", path, key, "application/json",
                json == null ? null : json.getBytes(StandardCharsets.UTF_8), headers);
    }

    /**
     * @param headers extra headers as name, value pairs
     */
    public Response send(String method, String path, String key, String contentType, byte[] body,
                         String... headers) {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofByteArray(body));
        if (key != null) {
            b.header("X-API-Key", key);
        }
        if (contentType != null) {
            b.header("Content-Type", contentType);
        }
        for (int i = 0; i + 1 < headers.length; i += 2) {
            b.header(headers[i], headers[i + 1]);
        }
        return send(b.build());
    }

    public Response send(HttpRequest request) {
        try {
            HttpResponse<String> r = HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new Response(r.statusCode(), r.headers(), r.body());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    public URI uri(String path) {
        return URI.create(base + path);
    }
}
