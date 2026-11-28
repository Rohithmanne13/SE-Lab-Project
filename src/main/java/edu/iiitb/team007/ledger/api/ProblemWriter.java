package edu.iiitb.team007.ledger.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.iiitb.team007.ledger.domain.ApiException;
import edu.iiitb.team007.ledger.domain.ErrorCode;
import edu.iiitb.team007.ledger.domain.FieldProblem;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Writes every error as an RFC 9457 problem document, {@code application/problem+json} (SRS 3.3.4, ADR-0004):
 * type, title, status, detail, plus the extensions code and request_id, and errors for VALIDATION_ERROR. Used by
 * the filters and by {@link ApiExceptionHandler}, so no error ever leaves in another format. It never writes
 * stack traces, SQL or file paths (SEC-6).
 */
@Component
public class ProblemWriter {

    /** Media type of every error response. */
    public static final String PROBLEM_JSON = "application/problem+json";

    /** Seconds a client should wait after SERVICE_BUSY (FR-3.6, ADR-0002). */
    public static final String RETRY_AFTER_SECONDS = "1";

    private final ObjectMapper mapper;

    public ProblemWriter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public void write(HttpServletRequest request, HttpServletResponse response, ApiException e) throws IOException {
        ErrorCode code = e.code();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", code.typeUri());
        body.put("title", code.title());
        body.put("status", code.status());
        body.put("detail", e.detail());
        body.put("code", code.name());
        body.put("request_id", requestId(request));
        if (!e.errors().isEmpty()) {
            body.put("errors", fieldErrors(e.errors()));
        }
        if (code == ErrorCode.SERVICE_BUSY) {
            response.setHeader("Retry-After", RETRY_AFTER_SECONDS);
        }
        send(response, code.status(), body);
    }

    /**
     * A response for a path or method the API does not define (404, 405, 406). It is not one of the SRS error
     * conditions, so it has type {@code about:blank}, whose title is the HTTP status phrase (RFC 9457 4.2.1).
     */
    public void writeHttpStatus(HttpServletRequest request, HttpServletResponse response, HttpStatus status,
                                String detail) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "about:blank");
        body.put("title", status.getReasonPhrase());
        body.put("status", status.value());
        body.put("detail", detail);
        body.put("request_id", requestId(request));
        send(response, status.value(), body);
    }

    private static List<Map<String, String>> fieldErrors(List<FieldProblem> errors) {
        return errors.stream().map(p -> {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("field", p.field());
            m.put("message", p.message());
            return m;
        }).toList();
    }

    private void send(HttpServletResponse response, int status, Map<String, Object> body) throws IOException {
        response.resetBuffer();
        response.setStatus(status);
        response.setContentType(PROBLEM_JSON);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getOutputStream().write(mapper.writeValueAsBytes(body));
        response.flushBuffer();
    }

    private static String requestId(HttpServletRequest request) {
        Object id = request.getAttribute(RequestAttributes.REQUEST_ID);
        return id == null ? null : id.toString();
    }
}
