package edu.iiitb.team007.ledger.persistence;

import edu.iiitb.team007.ledger.domain.Wallet;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** {@link WalletRepository} on SQLite with parameterised SQL (SI-1, SEC-3). */
@Repository
public class JdbcWalletRepository implements WalletRepository {

    private static final String COLUMNS = "wallet_id, user_id, currency, balance, is_system, created_at";

    private final LedgerJdbc db;

    public JdbcWalletRepository(LedgerJdbc db) {
        this.db = db;
    }

    @Override
    public Optional<Wallet> find(UUID walletId) {
        return db.template().query("SELECT " + COLUMNS + " FROM wallets WHERE wallet_id = ?",
                JdbcWalletRepository::map, walletId.toString()).stream().findFirst();
    }

    @Override
    public Optional<Wallet> findSystemWallet(String currency) {
        return db.template().query("SELECT " + COLUMNS + " FROM wallets WHERE is_system = 1 AND currency = ?",
                JdbcWalletRepository::map, currency).stream().findFirst();
    }

    @Override
    public List<Wallet> findByUser(UUID userId) {
        return db.template().query("SELECT " + COLUMNS + " FROM wallets WHERE user_id = ? ORDER BY currency",
                JdbcWalletRepository::map, userId.toString());
    }

    @Override
    public boolean exists(UUID userId, String currency) {
        Integer n = db.template().queryForObject("SELECT count(*) FROM wallets WHERE user_id = ? AND currency = ?",
                Integer.class, userId.toString(), currency);
        return n != null && n > 0;
    }

    @Override
    public void insert(Wallet wallet) {
        db.template().update("INSERT INTO wallets (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?)",
                wallet.walletId().toString(), wallet.userId().toString(), wallet.currency(), wallet.balance(),
                wallet.system() ? 1 : 0, wallet.createdAt());
    }

    @Override
    public void updateBalance(UUID walletId, long balance) {
        int rows = db.template().update("UPDATE wallets SET balance = ? WHERE wallet_id = ?", balance,
                walletId.toString());
        if (rows != 1) {
            throw new IllegalStateException("Balance update matched " + rows + " wallets");
        }
    }

    private static Wallet map(ResultSet rs, int row) throws SQLException {
        return new Wallet(UUID.fromString(rs.getString("wallet_id")), UUID.fromString(rs.getString("user_id")),
                rs.getString("currency"), rs.getLong("balance"), rs.getInt("is_system") == 1,
                rs.getString("created_at"));
    }
}
