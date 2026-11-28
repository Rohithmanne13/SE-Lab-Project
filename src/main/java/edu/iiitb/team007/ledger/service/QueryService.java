package edu.iiitb.team007.ledger.service;

import edu.iiitb.team007.ledger.domain.ApiException;
import edu.iiitb.team007.ledger.domain.ErrorCode;
import edu.iiitb.team007.ledger.domain.Wallet;
import edu.iiitb.team007.ledger.persistence.LedgerRepository;
import edu.iiitb.team007.ledger.persistence.TransactionRunner;
import edu.iiitb.team007.ledger.persistence.WalletRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Read-only queries (FR-6). Each runs in one read transaction, so it sees only committed state, never a
 * partially applied transaction (FR-6.5).
 */
@Service
public class QueryService {

    private final TransactionRunner transactions;
    private final WalletRepository wallets;
    private final LedgerRepository ledger;
    private final TransactionViews views;

    public QueryService(TransactionRunner transactions, WalletRepository wallets, LedgerRepository ledger,
                        TransactionViews views) {
        this.transactions = transactions;
        this.wallets = wallets;
        this.ledger = ledger;
        this.views = views;
    }

    /** A transaction with its type, amount, currency, timestamps, description and all entries (FR-6.3, FR-4.7). */
    public TransactionView transaction(UUID transactionId) {
        return transactions.inReadTransaction(() -> views.of(ledger.findTransaction(transactionId)
                .orElseThrow(() -> transactionNotFound(transactionId))));
    }

    /** A wallet with its current balance (FR-6.1); system wallets are not visible (FR-1.5). */
    public WalletView wallet(UUID walletId) {
        return transactions.inReadTransaction(() -> WalletView.of(clientWallet(walletId)));
    }

    /** One page of a wallet's entries in ascending sequence, with the total count (FR-6.2). */
    public EntryPageView entries(UUID walletId, int limit, int offset) {
        return transactions.inReadTransaction(() -> {
            Wallet wallet = clientWallet(walletId);
            return new EntryPageView(wallet.walletId(),
                    ledger.entriesOfWallet(wallet.walletId(), limit, offset).stream()
                            .map(e -> EntryView.of(e, false)).toList(),
                    ledger.countEntriesOfWallet(wallet.walletId()), limit, offset);
        });
    }

    private Wallet clientWallet(UUID walletId) {
        return wallets.find(walletId).filter(w -> !w.system()).orElseThrow(() -> walletNotFound(walletId));
    }

    static ApiException transactionNotFound(UUID transactionId) {
        return new ApiException(ErrorCode.TRANSACTION_NOT_FOUND,
                "No transaction with id " + transactionId + " exists.");
    }

    static ApiException walletNotFound(UUID walletId) {
        return new ApiException(ErrorCode.WALLET_NOT_FOUND, "No wallet with id " + walletId + " exists.");
    }
}
