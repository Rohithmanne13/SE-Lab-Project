package edu.iiitb.team007.ledger.support;

import edu.iiitb.team007.ledger.domain.LedgerEntry;

/**
 * The ledger hash chain (FR-4.4, ADR-0003). The one place that defines the serialisation:
 * <pre>
 * entry_hash = lowercase_hex(SHA-256(UTF-8(prev_hash + "\n" + sequence + "|" + entry_id + "|" +
 *              transaction_id + "|" + wallet_id + "|" + direction + "|" + amount + "|" + currency + "|" +
 *              created_at)))
 * </pre>
 * {@code prev_hash} is the entry hash of the entry with {@code sequence - 1}, or 64 zeros for the first entry.
 */
public final class HashChain {

    /** prev_hash of the first entry (sequence 1): 64 zeros. */
    public static final String GENESIS = "0".repeat(64);

    private HashChain() {
    }

    /** The exact text that is hashed, built from the stored values. */
    public static String serialise(String prevHash, long sequence, String entryId, String transactionId,
                                   String walletId, String direction, long amount, String currency,
                                   String createdAt) {
        return prevHash + "\n" + sequence + "|" + entryId + "|" + transactionId + "|" + walletId + "|"
                + direction + "|" + amount + "|" + currency + "|" + createdAt;
    }

    public static String entryHash(String prevHash, long sequence, String entryId, String transactionId,
                                   String walletId, String direction, long amount, String currency,
                                   String createdAt) {
        return Sha256.hex(serialise(prevHash, sequence, entryId, transactionId, walletId, direction, amount,
                currency, createdAt));
    }

    /** Hash of an entry from its own fields and its {@code prevHash}. */
    public static String entryHash(LedgerEntry e) {
        return entryHash(e.prevHash(), e.sequence(), e.entryId().toString(), e.transactionId().toString(),
                e.walletId().toString(), e.direction().name(), e.amount(), e.currency(), e.createdAt());
    }
}
