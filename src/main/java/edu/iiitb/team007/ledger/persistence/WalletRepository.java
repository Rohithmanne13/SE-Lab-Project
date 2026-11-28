package edu.iiitb.team007.ledger.persistence;

import edu.iiitb.team007.ledger.domain.Wallet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistence of wallets (DR-1, CON-5). Call only inside a {@link TransactionRunner} unit of work. */
public interface WalletRepository {

    Optional<Wallet> find(UUID walletId);

    /** The system funding account of a currency (FR-1.5). */
    Optional<Wallet> findSystemWallet(String currency);

    /** A user's wallets, ordered by currency. */
    List<Wallet> findByUser(UUID userId);

    boolean exists(UUID userId, String currency);

    void insert(Wallet wallet);

    /** Stores a new balance; only the posting service calls this, after writing the entries (FR-1.7). */
    void updateBalance(UUID walletId, long balance);
}
