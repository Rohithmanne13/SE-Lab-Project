package edu.iiitb.team007.ledger.persistence;

import edu.iiitb.team007.ledger.domain.LedgerEntry;
import edu.iiitb.team007.ledger.domain.LedgerTransaction;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Append-only persistence of transactions and entries (FR-4, CON-5). There is deliberately no update or
 * delete method (FR-4.1); the database triggers forbid them as well (FR-4.2).
 */
public interface LedgerRepository {

    /** The sequence number for the next entry: MAX(sequence) + 1, or 1 for an empty ledger (FR-4.3). */
    long nextSequence();

    /** entry_hash of the last entry, or 64 zeros for an empty ledger (FR-4.4). */
    String lastHash();

    void insertTransaction(LedgerTransaction transaction);

    void insertEntry(LedgerEntry entry);

    Optional<LedgerTransaction> findTransaction(UUID transactionId);

    /** The entries of a transaction in sequence order. */
    List<LedgerEntry> entriesOf(UUID transactionId);

    /** The REVERSAL that reverses this transaction, if any (FR-4.7). */
    Optional<UUID> reversalOf(UUID transactionId);

    /** One page of a wallet's entries in ascending sequence (FR-6.2). */
    List<LedgerEntry> entriesOfWallet(UUID walletId, int limit, int offset);

    long countEntriesOfWallet(UUID walletId);
}
