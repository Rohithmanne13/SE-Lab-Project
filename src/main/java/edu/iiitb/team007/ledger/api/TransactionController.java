package edu.iiitb.team007.ledger.api;

import edu.iiitb.team007.ledger.service.IdempotencyService;
import edu.iiitb.team007.ledger.service.IdempotentResult;
import edu.iiitb.team007.ledger.service.QueryService;
import edu.iiitb.team007.ledger.service.ReversalService;
import edu.iiitb.team007.ledger.service.TransactionView;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /transactions/{transactionId} (FR-6.3, FR-4.7) - role CLIENT - and POST /transactions/{transactionId}/reversal
 * (FR-4.5, FR-4.6) - role ADMIN, enforced by {@link ApiKeyFilter}. There is no PUT, PATCH or DELETE on transactions
 * or entries (FR-4.1).
 */
@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final QueryService queries;
    private final ReversalService reversals;
    private final JsonBodies bodies;

    public TransactionController(QueryService queries, ReversalService reversals, JsonBodies bodies) {
        this.queries = queries;
        this.reversals = reversals;
        this.bodies = bodies;
    }

    @GetMapping("/{transactionId}")
    public TransactionView get(@PathVariable String transactionId) {
        return queries.transaction(RequestIds.require("transactionId", transactionId));
    }

    @PostMapping("/{transactionId}/reversal")
    public ResponseEntity<String> reverse(HttpServletRequest request, @PathVariable String transactionId,
                                          @RequestHeader(value = IdempotencyKeys.HEADER, required = false) String key,
                                          @RequestBody(required = false) byte[] body) {
        bodies.requireJson(request);
        String idempotencyKey = IdempotencyKeys.require(key);
        UUID id = RequestIds.require("transactionId", transactionId);
        JsonBodies.Parsed<ReversalRequest> parsed = bodies.read(body, ReversalRequest.class);
        String fingerprint = IdempotencyService.fingerprint(request.getMethod(), request.getRequestURI(),
                parsed.tree());
        IdempotentResult result = reversals.reverse(idempotencyKey, fingerprint, id, parsed.value().reason());
        return MoneyResponses.of(result, request);
    }
}
