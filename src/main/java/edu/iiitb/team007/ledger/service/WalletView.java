package edu.iiitb.team007.ledger.service;

import edu.iiitb.team007.ledger.domain.Wallet;
import java.util.UUID;

/** JSON of a wallet (FR-1.3, FR-6.1): wallet_id, user_id, currency, balance (integer minor units), created_at. */
public record WalletView(UUID walletId, UUID userId, String currency, long balance, String createdAt) {

    public static WalletView of(Wallet w) {
        return new WalletView(w.walletId(), w.userId(), w.currency(), w.balance(), w.createdAt());
    }
}
