package edu.iiitb.team007.ledger.api;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.iiitb.team007.ledger.testsupport.ApiClient;
import edu.iiitb.team007.ledger.testsupport.IntegrationTestSupport;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/** One structured JSON log line per request, without keys or bodies (NFR-OBS-1, SEC-7). */
class AccessLogTest extends IntegrationTestSupport {

    private final ObjectMapper mapper = new ObjectMapper();
    private final Logger access = (Logger) LoggerFactory.getLogger("ledger.access");
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void capture() {
        appender = new ListAppender<>();
        appender.start();
        access.addAppender(appender);
    }

    @AfterEach
    void release() {
        access.detachAppender(appender);
    }

    private List<JsonNode> linesFor(String requestId) throws Exception {
        List<JsonNode> lines = new java.util.ArrayList<>();
        for (ILoggingEvent e : appender.list) {
            JsonNode line = mapper.readTree(e.getFormattedMessage());
            if (requestId.equals(line.get("request_id").asText())) {
                lines.add(line);
            }
        }
        return lines;
    }

    @Test
    void everyRequestLogsOneJsonLineWithTheRequiredFields() throws Exception {
        String secretName = "Private Person";
        ApiClient.Response r = api().post("/api/v1/users", CLIENT,
                "{\"name\":\"" + secretName + "\",\"email\":\"" + uniqueEmail() + "\"}", "X-Request-ID", "log-test-1");
        assertThat(r.status()).isEqualTo(201);

        List<JsonNode> lines = linesFor("log-test-1");
        assertThat(lines).hasSize(1);
        JsonNode line = lines.get(0);
        assertThat(line.get("timestamp").asText()).endsWith("Z");
        assertThat(line.get("method").asText()).isEqualTo("POST");
        assertThat(line.get("path").asText()).isEqualTo("/api/v1/users");
        assertThat(line.get("status").asInt()).isEqualTo(201);
        assertThat(line.get("latency_ms").isIntegralNumber()).isTrue();
        String all = appender.list.stream().map(ILoggingEvent::getFormattedMessage).reduce("", String::concat);
        assertThat(all).doesNotContain(CLIENT).doesNotContain(ADMIN).doesNotContain(secretName);
    }

    @Test
    void rejectedRequestsAreLoggedToo() throws Exception {
        api().get("/api/v1/users/x", "bad-key", "X-Request-ID", "log-test-2");
        List<JsonNode> lines = linesFor("log-test-2");
        assertThat(lines).hasSize(1);
        assertThat(lines.get(0).get("status").asInt()).isEqualTo(401);
        assertThat(appender.list.stream().map(ILoggingEvent::getFormattedMessage).reduce("", String::concat))
                .doesNotContain("bad-key");
    }
}
