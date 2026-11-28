package edu.iiitb.team007.ledger.api;

import edu.iiitb.team007.ledger.service.IdempotentResult;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * The HTTP response of a money-moving request: the stored status and exact body (byte for byte the same on every
 * replay, NFR-REL-4), Location of the transaction (CI-3) and Idempotent-Replayed: true on a replay (CI-4).
 */
final class MoneyResponses {

    /** Header on replayed responses (CI-4). */
    static final String REPLAYED = "Idempotent-Replayed";

    private MoneyResponses() {
    }

    static ResponseEntity<String> of(IdempotentResult result, HttpServletRequest request) {
        request.setAttribute(RequestAttributes.TRANSACTION_ID, result.transactionId());
        ResponseEntity.BodyBuilder b = ResponseEntity.status(result.status())
                .contentType(MediaType.APPLICATION_JSON)
                .location(URI.create("/api/v1/transactions/" + result.transactionId()));
        if (result.replayed()) {
            b.header(REPLAYED, "true");
        }
        return b.body(result.body());
    }
}
