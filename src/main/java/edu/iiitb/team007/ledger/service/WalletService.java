package edu.iiitb.team007.ledger.service;

import edu.iiitb.team007.ledger.config.SupportedCurrencies;
import edu.iiitb.team007.ledger.domain.ApiException;
import edu.iiitb.team007.ledger.domain.ErrorCode;
import edu.iiitb.team007.ledger.domain.User;
import edu.iiitb.team007.ledger.domain.Wallet;
import edu.iiitb.team007.ledger.persistence.TransactionRunner;
import edu.iiitb.team007.ledger.persistence.UserRepository;
import edu.iiitb.team007.ledger.persistence.WalletRepository;
import edu.iiitb.team007.ledger.support.IdGenerator;
import edu.iiitb.team007.ledger.support.Timestamps;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Wallets of a user (FR-1.3, FR-1.4, FR-6.1). A new wallet starts at balance 0 (FR-1, FR-1.7). */
@Service
public class WalletService {

    private final TransactionRunner transactions;
    private final UserRepository users;
    private final WalletRepository wallets;
    private final SupportedCurrencies currencies;
    private final IdGenerator ids;
    private final Clock clock;

    public WalletService(TransactionRunner transactions, UserRepository users, WalletRepository wallets,
                         SupportedCurrencies currencies, IdGenerator ids, Clock clock) {
        this.transactions = transactions;
        this.users = users;
        this.wallets = wallets;
        this.currencies = currencies;
        this.ids = ids;
        this.clock = clock;
    }

    /**
     * Creates a wallet: unknown user 404 USER_NOT_FOUND, unsupported currency 422 UNSUPPORTED_CURRENCY, second
     * wallet in a currency 409 WALLET_ALREADY_EXISTS (BR-6).
     */
    public WalletView create(UUID userId, String currency) {
        return transactions.inWriteTransaction(() -> {
            User user = clientUser(userId);
            if (!currencies.isSupported(currency)) {
                throw new ApiException(ErrorCode.UNSUPPORTED_CURRENCY, "The currency " + currency
                        + " is not supported; supported currencies are " + String.join(", ", currencies.all()) + ".");
            }
            if (wallets.exists(user.userId(), currency)) {
                throw new ApiException(ErrorCode.WALLET_ALREADY_EXISTS,
                        "This user already has a wallet in " + currency + "; a user has one wallet per currency.");
            }
            Wallet wallet = new Wallet(ids.newId(), user.userId(), currency, 0, false, Timestamps.now(clock));
            wallets.insert(wallet);
            return WalletView.of(wallet);
        });
    }

    /** All wallets of a user, ordered by currency. */
    public WalletListView list(UUID userId) {
        return transactions.inReadTransaction(() -> {
            User user = clientUser(userId);
            return new WalletListView(user.userId(),
                    wallets.findByUser(user.userId()).stream().map(WalletView::of).toList());
        });
    }

    private User clientUser(UUID userId) {
        return users.find(userId).filter(u -> !u.system()).orElseThrow(() -> UserService.notFound(userId));
    }
}
