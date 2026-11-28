package edu.iiitb.team007.ledger.service;

import edu.iiitb.team007.ledger.domain.TransactionType;
import java.util.List;
import java.util.UUID;

/**
 * JSON of a ledger transaction (FR-2.2, FR-1.6, FR-4.5, FR-4.7, FR-6.3). {@code source_wallet_id} is the wallet
 * debited and {@code destination_wallet_id} the wallet credited; either is null when it is a system funding
 * account (FR-1.5). {@code reverses_transaction_id} is set on a REVERSAL; {@code reversed_by_transaction_id} on a
 * transaction that has been reversed.
 */
public record TransactionView(UUID transactionId, TransactionType type, String status, UUID sourceWalletId,
                              UUID destinationWalletId, long amount, String currency, String description,
                              UUID reversesTransactionId, UUID reversedByTransactionId, String createdAt,
                              List<EntryView> entries) {

    /** Every committed transaction is COMPLETED in release 1.0. */
    public static final String COMPLETED = "COMPLETED";
}
