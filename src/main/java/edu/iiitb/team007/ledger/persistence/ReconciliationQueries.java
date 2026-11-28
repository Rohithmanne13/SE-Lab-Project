package edu.iiitb.team007.ledger.persistence;

import java.util.List;
import java.util.function.Consumer;

/**
 * Read-only queries used by reconciliation (FR-5.1-FR-5.5, CON-5). Call them inside one read transaction so they
 * all see the same snapshot.
 */
public interface ReconciliationQueries {

    /** Sums per currency over all entries. */
    record CurrencySums(String currency, long debits, long credits) {
    }

    /** One transaction with the facts about its entries. */
    record TransactionSums(String transactionId, String currency, long entryCount, long debits, long credits,
                           long entryCurrencies, String entryCurrency) {
    }

    /** One wallet with its stored balance and the balance computed from its entries. */
    record WalletSums(String walletId, long storedBalance, boolean system, long entryBalance) {
    }

    /** One entry exactly as stored, for recomputing the hash chain. */
    record StoredEntry(String entryId, long sequence, String transactionId, String walletId, String direction,
                       long amount, String currency, String createdAt, String prevHash, String entryHash) {
    }

    long countTransactions();

    long countEntries();

    long countWallets();

    List<CurrencySums> currencySums();

    List<TransactionSums> transactionSums();

    List<WalletSums> walletSums();

    /** Streams every entry in ascending sequence, without loading them all into memory. */
    void forEachEntryInSequence(Consumer<StoredEntry> consumer);
}
