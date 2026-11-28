package edu.iiitb.team007.ledger.service;

import edu.iiitb.team007.ledger.domain.Direction;
import edu.iiitb.team007.ledger.domain.LedgerEntry;
import edu.iiitb.team007.ledger.domain.LedgerTransaction;
import edu.iiitb.team007.ledger.domain.Wallet;
import edu.iiitb.team007.ledger.persistence.LedgerRepository;
import edu.iiitb.team007.ledger.persistence.WalletRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Builds {@link TransactionView}s inside a unit of work, hiding system funding account IDs (FR-1.5). */
@Component
public class TransactionViews {

    private final LedgerRepository ledger;
    private final WalletRepository wallets;

    public TransactionViews(LedgerRepository ledger, WalletRepository wallets) {
        this.ledger = ledger;
        this.wallets = wallets;
    }

    /** The view of a stored transaction, with its entries and reversed_by link (FR-6.3, FR-4.7). */
    public TransactionView of(LedgerTransaction t) {
        return of(t, ledger.entriesOf(t.transactionId()), ledger.reversalOf(t.transactionId()).orElse(null));
    }

    public TransactionView of(LedgerTransaction t, List<LedgerEntry> entries, UUID reversedBy) {
        Map<UUID, Boolean> system = new HashMap<>();
        for (LedgerEntry e : entries) {
            system.computeIfAbsent(e.walletId(), id -> wallets.find(id).map(Wallet::system).orElse(false));
        }
        UUID source = null;
        UUID destination = null;
        for (LedgerEntry e : entries) {
            UUID visible = system.get(e.walletId()) ? null : e.walletId();
            if (e.direction() == Direction.DEBIT) {
                source = visible;
            } else {
                destination = visible;
            }
        }
        return new TransactionView(t.transactionId(), t.type(), TransactionView.COMPLETED, source, destination,
                t.amount(), t.currency(), t.description(), t.reversesTransactionId(), reversedBy, t.createdAt(),
                entries.stream().map(e -> EntryView.of(e, system.get(e.walletId()))).toList());
    }
}
