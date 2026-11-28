package edu.iiitb.team007.ledger.domain;

import java.util.UUID;

/**
 * One immutable DEBIT or CREDIT line on one wallet (FR-4.3). {@code sequence} is global, strictly
 * increasing and gap-free; {@code entryHash} chains the entry to the previous one (FR-4.4, ADR-0003).
 */
public record LedgerEntry(UUID entryId, long sequence, UUID transactionId, UUID walletId, Direction direction,
                          long amount, String currency, String createdAt, String prevHash, String entryHash) {
}
