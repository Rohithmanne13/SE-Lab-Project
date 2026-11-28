package edu.iiitb.team007.ledger.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.iiitb.team007.ledger.domain.ApiException;
import edu.iiitb.team007.ledger.domain.ErrorCode;
import edu.iiitb.team007.ledger.domain.IdempotencyRecord;
import edu.iiitb.team007.ledger.persistence.IdempotencyRepository;
import edu.iiitb.team007.ledger.persistence.TransactionRunner;
import edu.iiitb.team007.ledger.support.CanonicalJson;
import edu.iiitb.team007.ledger.support.RequestFingerprint;
import edu.iiitb.team007.ledger.support.Timestamps;
import java.time.Clock;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

/**
 * Idempotent execution of deposits, transfers and reversals (FR-2.3-FR-2.9, BR-9, ADR-0002). In ONE write
 * transaction it looks the key up, replays the stored response for the same fingerprint or answers 422 for a
 * different one, and otherwise posts the transaction and stores the key, fingerprint, transaction, status and
 * exact response body - all committed together or not at all (FR-2.5). A failure rolls back, so it stores no
 * record and the client may retry with the same key (FR-2.8).
 */
@Service
public class IdempotencyService {

    /** Status of a successful money-moving request (CI-3). */
    public static final int CREATED = 201;

    private final TransactionRunner transactions;
    private final IdempotencyRepository records;
    private final ObjectMapper mapper;
    private final Clock clock;

    public IdempotencyService(TransactionRunner transactions, IdempotencyRepository records, ObjectMapper mapper,
                              Clock clock) {
        this.transactions = transactions;
        this.records = records;
        this.mapper = mapper;
        this.clock = clock;
    }

    /** FR-2.4: SHA-256 of the method, the request path and the canonical JSON body. */
    public static String fingerprint(String method, String path, JsonNode body) {
        return RequestFingerprint.of(method, path, CanonicalJson.of(body));
    }

    /**
     * @param post the posting work; it runs inside the same write transaction, after the key lookup
     */
    public IdempotentResult execute(String key, String fingerprint, Supplier<TransactionView> post) {
        return transactions.inWriteTransaction(() -> {
            Optional<IdempotencyRecord> stored = records.find(key);
            if (stored.isPresent()) {
                IdempotencyRecord r = stored.get();
                if (!r.requestHash().equals(fingerprint)) {
                    throw new ApiException(ErrorCode.IDEMPOTENCY_KEY_REUSED, "This Idempotency-Key was already used "
                            + "for a different request (method, path or body); use a new key for a new request.");
                }
                return new IdempotentResult(r.responseStatus(), r.responseBody(), r.transactionId(), true);
            }
            TransactionView view = post.get();
            String body = json(view);
            records.insert(new IdempotencyRecord(key, fingerprint, view.transactionId(), CREATED, body,
                    Timestamps.now(clock)));
            return new IdempotentResult(CREATED, body, view.transactionId(), false);
        });
    }

    private String json(TransactionView view) {
        try {
            return mapper.writeValueAsString(view);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialise the response", e);
        }
    }
}
