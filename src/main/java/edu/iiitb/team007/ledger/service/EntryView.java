package edu.iiitb.team007.ledger.service;

import edu.iiitb.team007.ledger.domain.Direction;
import edu.iiitb.team007.ledger.domain.LedgerEntry;
import java.util.UUID;

/**
 * JSON of a ledger entry (FR-4.3). {@code wallet_id} is null for the entry on a system funding account, whose
 * identifier is never returned by the API (FR-1.5).
 */
public record EntryView(UUID entryId, long sequence, UUID transactionId, UUID walletId, Direction direction,
                        long amount, String currency, String createdAt, String prevHash, String entryHash) {

    public static EntryView of(LedgerEntry e, boolean systemWallet) {
        return new EntryView(e.entryId(), e.sequence(), e.transactionId(), systemWallet ? null : e.walletId(),
                e.direction(), e.amount(), e.currency(), e.createdAt(), e.prevHash(), e.entryHash());
    }
}
