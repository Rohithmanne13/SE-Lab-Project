package edu.iiitb.team007.ledger.service;

import edu.iiitb.team007.ledger.domain.ApiException;
import edu.iiitb.team007.ledger.domain.ErrorCode;
import edu.iiitb.team007.ledger.domain.LedgerEntry;
import edu.iiitb.team007.ledger.domain.LedgerTransaction;
import edu.iiitb.team007.ledger.domain.TransactionType;
import edu.iiitb.team007.ledger.domain.Wallet;
import edu.iiitb.team007.ledger.persistence.LedgerRepository;
import edu.iiitb.team007.ledger.persistence.WalletRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Administrative reversals (FR-4.5, FR-4.6, BR-4, BR-7): a new REVERSAL transaction that references the original
 * and has, for every original entry, an entry with the same wallet and amount and the opposite direction. The
 * original transaction and its entries never change. Idempotent like transfers (FR-2.3-FR-2.8).
 */
@Service
public class ReversalService {

    private final IdempotencyService idempotency;
    private final LedgerRepository ledger;
    private final WalletRepository wallets;
    private final PostingService posting;
    private final TransactionViews views;

    public ReversalService(IdempotencyService idempotency, LedgerRepository ledger, WalletRepository wallets,
                           PostingService posting, TransactionViews views) {
        this.idempotency = idempotency;
        this.ledger = ledger;
        this.wallets = wallets;
        this.posting = posting;
        this.views = views;
    }

    public IdempotentResult reverse(String key, String fingerprint, UUID transactionId, String reason) {
        return idempotency.execute(key, fingerprint, () -> {
            LedgerTransaction original = ledger.findTransaction(transactionId)
                    .orElseThrow(() -> QueryService.transactionNotFound(transactionId));
            if (original.type() == TransactionType.REVERSAL) {
                throw new ApiException(ErrorCode.TRANSACTION_NOT_REVERSIBLE,
                        "Transaction " + transactionId + " is itself a reversal; a reversal cannot be reversed.");
            }
            if (ledger.reversalOf(transactionId).isPresent()) {
                throw new ApiException(ErrorCode.TRANSACTION_ALREADY_REVERSED,
                        "Transaction " + transactionId + " has already been reversed; it can be reversed only once.");
            }
            List<Leg> legs = new ArrayList<>();
            for (LedgerEntry e : ledger.entriesOf(transactionId)) {
                Wallet wallet = wallets.find(e.walletId())
                        .orElseThrow(() -> new IllegalStateException("Entry on a missing wallet " + e.walletId()));
                legs.add(new Leg(wallet, e.direction().opposite(), e.amount()));
            }
            PostingService.Posted posted = posting.post(TransactionType.REVERSAL, original.currency(),
                    original.amount(), reason, original.transactionId(), legs);
            return views.of(posted.transaction(), posted.entries(), null);
        });
    }
}
