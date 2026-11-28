package edu.iiitb.team007.ledger.service;

import java.util.UUID;

/**
 * The outcome of an idempotent money-moving request (FR-2.5, FR-2.6): the HTTP status and exact JSON body to
 * send, the transaction, and whether it is a replay of a stored response.
 */
public record IdempotentResult(int status, String body, UUID transactionId, boolean replayed) {
}
