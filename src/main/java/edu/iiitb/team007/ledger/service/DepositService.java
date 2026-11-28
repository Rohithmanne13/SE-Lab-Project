package edu.iiitb.team007.ledger.service;

import edu.iiitb.team007.ledger.config.SupportedCurrencies;
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
 * Administrative deposits (FR-1.6, AS-3): in one ACID transaction a DEPOSIT debits the currency's system funding
 * account and credits the wallet with the same amount. Idempotent like transfers (FR-2.3-FR-2.8).
 */
@Service
public class DepositService {

    private final IdempotencyService idempotency;
    private final WalletRepository wallets;
    private final SystemAccounts systemAccounts;
    private final SupportedCurrencies currencies;
    private final PostingService posting;
    private final TransactionViews views;

    public DepositService(IdempotencyService idempotency, WalletRepository wallets, SystemAccounts systemAccounts,
                          SupportedCurrencies currencies, PostingService posting, TransactionViews views) {
        this.idempotency = idempotency;
        this.wallets = wallets;
        this.systemAccounts = systemAccounts;
        this.currencies = currencies;
        this.posting = posting;
        this.views = views;
    }

    public IdempotentResult deposit(String key, String fingerprint, UUID walletId, long amount, String currency,
                                    String description) {
        return idempotency.execute(key, fingerprint, () -> {
            Wallet wallet = wallets.find(walletId).filter(w -> !w.system())
                    .orElseThrow(() -> QueryService.walletNotFound(walletId));
            if (!currencies.isSupported(currency)) {
                throw new ApiException(ErrorCode.UNSUPPORTED_CURRENCY, "The currency " + currency
                        + " is not supported; supported currencies are " + String.join(", ", currencies.all()) + ".");
            }
            if (!wallet.currency().equals(currency)) {
                throw new ApiException(ErrorCode.CURRENCY_MISMATCH, "The wallet holds " + wallet.currency()
                        + " but the deposit is in " + currency + "; deposit in the wallet's currency.");
            }
            Wallet funding = systemAccounts.fundingWallet(currency);
            PostingService.Posted posted = posting.post(TransactionType.DEPOSIT, currency, amount, description, null,
                    List.of(new Leg(funding, Direction.DEBIT, amount), new Leg(wallet, Direction.CREDIT, amount)));
            return views.of(posted.transaction(), posted.entries(), null);
        });
    }
}
