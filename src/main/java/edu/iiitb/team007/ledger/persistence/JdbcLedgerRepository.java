package edu.iiitb.team007.ledger.persistence;

import edu.iiitb.team007.ledger.domain.Direction;
import edu.iiitb.team007.ledger.domain.LedgerEntry;
import edu.iiitb.team007.ledger.domain.LedgerTransaction;
import edu.iiitb.team007.ledger.domain.TransactionType;
import edu.iiitb.team007.ledger.support.HashChain;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** {@link LedgerRepository} on SQLite: INSERT and SELECT only (FR-4.1), parameterised SQL (SEC-3). */
@Repository
public class JdbcLedgerRepository implements LedgerRepository {

    private static final String TXN_COLUMNS =
            "transaction_id, type, status, currency, amount, description, reverses_transaction_id, created_at";
    private static final String ENTRY_COLUMNS = "entry_id, sequence, transaction_id, wallet_id, direction, amount, "
            + "currency, created_at, prev_hash, entry_hash";

    private final LedgerJdbc db;

    public JdbcLedgerRepository(LedgerJdbc db) {
        this.db = db;
    }

    @Override
    public long nextSequence() {
        Long next = db.template().queryForObject("SELECT COALESCE(MAX(sequence), 0) + 1 FROM ledger_entries",
                Long.class);
        return next == null ? 1 : next;
    }

    @Override
    public String lastHash() {
        return db.template().query("SELECT entry_hash FROM ledger_entries ORDER BY sequence DESC LIMIT 1",
                (rs, row) -> rs.getString(1)).stream().findFirst().orElse(HashChain.GENESIS);
    }

    @Override
    public void insertTransaction(LedgerTransaction t) {
        db.template().update("INSERT INTO transactions (" + TXN_COLUMNS + ") VALUES (?, ?, 'COMPLETED', ?, ?, ?, ?, ?)",
                t.transactionId().toString(), t.type().name(), t.currency(), t.amount(), t.description(),
                t.reversesTransactionId() == null ? null : t.reversesTransactionId().toString(), t.createdAt());
    }

    @Override
    public void insertEntry(LedgerEntry e) {
        db.template().update("INSERT INTO ledger_entries (" + ENTRY_COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                e.entryId().toString(), e.sequence(), e.transactionId().toString(), e.walletId().toString(),
                e.direction().name(), e.amount(), e.currency(), e.createdAt(), e.prevHash(), e.entryHash());
    }

    @Override
    public Optional<LedgerTransaction> findTransaction(UUID transactionId) {
        return db.template().query("SELECT " + TXN_COLUMNS + " FROM transactions WHERE transaction_id = ?",
                JdbcLedgerRepository::mapTransaction, transactionId.toString()).stream().findFirst();
    }

    @Override
    public List<LedgerEntry> entriesOf(UUID transactionId) {
        return db.template().query("SELECT " + ENTRY_COLUMNS
                        + " FROM ledger_entries WHERE transaction_id = ? ORDER BY sequence",
                JdbcLedgerRepository::mapEntry, transactionId.toString());
    }

    @Override
    public Optional<UUID> reversalOf(UUID transactionId) {
        return db.template().query("SELECT transaction_id FROM transactions WHERE reverses_transaction_id = ?",
                (rs, row) -> UUID.fromString(rs.getString(1)), transactionId.toString()).stream().findFirst();
    }

    @Override
    public List<LedgerEntry> entriesOfWallet(UUID walletId, int limit, int offset) {
        return db.template().query("SELECT " + ENTRY_COLUMNS
                        + " FROM ledger_entries WHERE wallet_id = ? ORDER BY sequence LIMIT ? OFFSET ?",
                JdbcLedgerRepository::mapEntry, walletId.toString(), limit, offset);
    }

    @Override
    public long countEntriesOfWallet(UUID walletId) {
        Long n = db.template().queryForObject("SELECT count(*) FROM ledger_entries WHERE wallet_id = ?", Long.class,
                walletId.toString());
        return n == null ? 0 : n;
    }

    static LedgerTransaction mapTransaction(ResultSet rs, int row) throws SQLException {
        String reverses = rs.getString("reverses_transaction_id");
        return new LedgerTransaction(UUID.fromString(rs.getString("transaction_id")),
                TransactionType.valueOf(rs.getString("type")), rs.getString("currency"), rs.getLong("amount"),
                rs.getString("description"), reverses == null ? null : UUID.fromString(reverses),
                rs.getString("created_at"));
    }

    static LedgerEntry mapEntry(ResultSet rs, int row) throws SQLException {
        return new LedgerEntry(UUID.fromString(rs.getString("entry_id")), rs.getLong("sequence"),
                UUID.fromString(rs.getString("transaction_id")), UUID.fromString(rs.getString("wallet_id")),
                Direction.valueOf(rs.getString("direction")), rs.getLong("amount"), rs.getString("currency"),
                rs.getString("created_at"), rs.getString("prev_hash"), rs.getString("entry_hash"));
    }
}
