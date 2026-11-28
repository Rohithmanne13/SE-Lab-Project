package edu.iiitb.team007.ledger.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.iiitb.team007.ledger.support.IdGenerator;
import edu.iiitb.team007.ledger.support.Timestamps;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * First filter of the pipeline (ADR-0004): takes the caller's X-Request-ID if it is 1-64 printable ASCII
 * characters, otherwise generates one, and returns it on every response (FR-0.4). After the request it logs one
 * structured JSON line - timestamp, request ID, method, path, status, latency and, for money-moving requests, the
 * transaction ID (NFR-OBS-1). It never logs API keys or bodies (SEC-7).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestIdFilter extends OncePerRequestFilter {

    /** Header name (SRS 3.3.3). */
    public static final String HEADER = "X-Request-ID";

    private static final Pattern VALID = Pattern.compile("^[\\x20-\\x7E]{1,64}$");
    private static final Logger ACCESS = LoggerFactory.getLogger("ledger.access");

    private final IdGenerator ids;
    private final Clock clock;
    private final ObjectMapper mapper;

    public RequestIdFilter(IdGenerator ids, Clock clock, ObjectMapper mapper) {
        this.ids = ids;
        this.clock = clock;
        this.mapper = mapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long start = System.nanoTime();
        String supplied = request.getHeader(HEADER);
        String requestId = supplied != null && VALID.matcher(supplied).matches() ? supplied : ids.newId().toString();
        request.setAttribute(RequestAttributes.REQUEST_ID, requestId);
        response.setHeader(HEADER, requestId);
        try {
            chain.doFilter(request, response);
        } finally {
            log(request, response, requestId, (System.nanoTime() - start) / 1_000_000);
        }
    }

    private void log(HttpServletRequest request, HttpServletResponse response, String requestId, long latencyMs) {
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("timestamp", Timestamps.now(clock));
        line.put("request_id", requestId);
        line.put("method", request.getMethod());
        line.put("path", request.getRequestURI());
        line.put("status", response.getStatus());
        line.put("latency_ms", latencyMs);
        Object transactionId = request.getAttribute(RequestAttributes.TRANSACTION_ID);
        if (transactionId != null) {
            line.put("transaction_id", transactionId.toString());
        }
        try {
            ACCESS.info(mapper.writeValueAsString(line));
        } catch (IOException e) {
            ACCESS.warn("{\"request_id\":\"{}\",\"log_error\":\"cannot write access log line\"}", requestId);
        }
    }
}
