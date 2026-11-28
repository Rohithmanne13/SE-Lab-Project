package edu.iiitb.team007.ledger.domain;

import java.util.UUID;

/**
 * A ledger transaction: a balanced group of entries committed atomically (DR-1, BR-1).
 *
 * @param amount                 the amount moved, in integer minor units (> 0)
 * @param description            optional text; for a REVERSAL it holds the reason (FR-4.5)
 * @param reversesTransactionId  the reversed transaction, only for type REVERSAL (FR-4.5)
 */
public record LedgerTransaction(UUID transactionId, TransactionType type, String currency, long amount,
                                String description, UUID reversesTransactionId, String createdAt) {
}
