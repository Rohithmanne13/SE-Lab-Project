package edu.iiitb.team007.ledger.service;

import edu.iiitb.team007.ledger.domain.ApiException;
import edu.iiitb.team007.ledger.domain.Direction;
import edu.iiitb.team007.ledger.domain.ErrorCode;
import edu.iiitb.team007.ledger.domain.TransactionType;
import edu.iiitb.team007.ledger.domain.Wallet;
import edu.iiitb.team007.ledger.persistence.WalletRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Idempotent, concurrency-safe transfers (FR-2, FR-3). Everything - key lookup, wallet checks, the funds check on
 * the balance read inside the transaction, entries, balances and the idempotency record - runs in one
 * BEGIN IMMEDIATE transaction, so concurrent transfers execute one at a time (FR-3.1-FR-3.3).
 */
@Service
public class TransferService {

    private final IdempotencyService idempotency;
    private final WalletRepository wallets;
    private final PostingService posting;
    private final TransactionViews views;

    public TransferService(IdempotencyService idempotency, WalletRepository wallets, PostingService posting,
                           TransactionViews views) {
        this.idempotency = idempotency;
        this.wallets = wallets;
        this.posting = posting;
        this.views = views;
    }

    public IdempotentResult transfer(String key, String fingerprint, UUID sourceId, UUID destinationId, long amount,
                                     String currency, String description) {
        return idempotency.execute(key, fingerprint, () -> {
            Wallet source = clientWallet(sourceId);
            Wallet destination = clientWallet(destinationId);
            if (source.walletId().equals(destination.walletId())) {
                throw new ApiException(ErrorCode.SAME_WALLET_TRANSFER,
                        "The source and destination wallet are the same; choose two different wallets.");
            }
            if (!source.currency().equals(currency) || !destination.currency().equals(currency)) {
                throw new ApiException(ErrorCode.CURRENCY_MISMATCH, "The transfer is in " + currency
                        + " but the source wallet holds " + source.currency() + " and the destination wallet holds "
                        + destination.currency() + "; money moves only between wallets of the same currency.");
            }
            if (source.balance() < amount) {
                throw new ApiException(ErrorCode.INSUFFICIENT_FUNDS, "Wallet " + source.walletId() + " holds "
                        + source.balance() + " but the transfer needs " + amount + "; nothing was changed.");
            }
            PostingService.Posted posted = posting.post(TransactionType.TRANSFER, currency, amount, description, null,
                    List.of(new Leg(source, Direction.DEBIT, amount), new Leg(destination, Direction.CREDIT, amount)));
            return views.of(posted.transaction(), posted.entries(), null);
        });
    }

    private Wallet clientWallet(UUID id) {
        return wallets.find(id).filter(w -> !w.system()).orElseThrow(() -> QueryService.walletNotFound(id));
    }
}
