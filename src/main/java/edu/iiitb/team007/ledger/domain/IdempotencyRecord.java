package edu.iiitb.team007.ledger.domain;

import java.util.UUID;

/**
 * A stored idempotency key with the fingerprint of the request that used it and the response that was sent
 * (FR-2.4, FR-2.5). Kept forever in release 1.0 (FR-2.9).
 *
 * @param requestHash    SHA-256 fingerprint of method, path and canonical body (FR-2.4)
 * @param responseBody   the exact JSON sent with the first response, replayed byte for byte (NFR-REL-4)
 */
public record IdempotencyRecord(String key, String requestHash, UUID transactionId, int responseStatus,
                                String responseBody, String createdAt) {
}
