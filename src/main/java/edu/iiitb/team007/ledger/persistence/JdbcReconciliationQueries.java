package edu.iiitb.team007.ledger.persistence;

import java.util.List;
import java.util.function.Consumer;
import org.springframework.stereotype.Repository;

/** {@link ReconciliationQueries} on SQLite: SELECT only (FR-5.1). */
@Repository
public class JdbcReconciliationQueries implements ReconciliationQueries {

    private static final String DEBIT = "SUM(CASE WHEN e.direction = 'DEBIT' THEN e.amount ELSE 0 END)";
    private static final String CREDIT = "SUM(CASE WHEN e.direction = 'CREDIT' THEN e.amount ELSE 0 END)";

    private final LedgerJdbc db;

    public JdbcReconciliationQueries(LedgerJdbc db) {
        this.db = db;
    }

    @Override
    public long countTransactions() {
        return count("transactions");
    }

    @Override
    public long countEntries() {
        return count("ledger_entries");
    }

    @Override
    public long countWallets() {
        return count("wallets");
    }

    private long count(String table) {
        Long n = db.template().queryForObject("SELECT count(*) FROM " + table, Long.class);
        return n == null ? 0 : n;
    }

    @Override
    public List<CurrencySums> currencySums() {
        return db.template().query("SELECT e.currency, " + DEBIT + ", " + CREDIT
                        + " FROM ledger_entries e GROUP BY e.currency ORDER BY e.currency",
                (rs, row) -> new CurrencySums(rs.getString(1), rs.getLong(2), rs.getLong(3)));
    }

    @Override
    public List<TransactionSums> transactionSums() {
        return db.template().query("SELECT t.transaction_id, t.currency, count(e.entry_id), COALESCE(" + DEBIT
                        + ", 0), COALESCE(" + CREDIT + ", 0), count(DISTINCT e.currency), min(e.currency)"
                        + " FROM transactions t LEFT JOIN ledger_entries e ON e.transaction_id = t.transaction_id"
                        + " GROUP BY t.transaction_id ORDER BY t.transaction_id",
                (rs, row) -> new TransactionSums(rs.getString(1), rs.getString(2), rs.getLong(3), rs.getLong(4),
                        rs.getLong(5), rs.getLong(6), rs.getString(7)));
    }

    @Override
    public List<WalletSums> walletSums() {
        return db.template().query("SELECT w.wallet_id, w.balance, w.is_system, COALESCE(" + CREDIT + ", 0) - "
                        + "COALESCE(" + DEBIT + ", 0) FROM wallets w LEFT JOIN ledger_entries e"
                        + " ON e.wallet_id = w.wallet_id GROUP BY w.wallet_id ORDER BY w.wallet_id",
                (rs, row) -> new WalletSums(rs.getString(1), rs.getLong(2), rs.getInt(3) == 1, rs.getLong(4)));
    }

    @Override
    public void forEachEntryInSequence(Consumer<StoredEntry> consumer) {
        db.template().query("SELECT entry_id, sequence, transaction_id, wallet_id, direction, amount, currency, "
                + "created_at, prev_hash, entry_hash FROM ledger_entries ORDER BY sequence", rs -> {
                    consumer.accept(new StoredEntry(rs.getString(1), rs.getLong(2), rs.getString(3), rs.getString(4),
                            rs.getString(5), rs.getLong(6), rs.getString(7), rs.getString(8), rs.getString(9),
                            rs.getString(10)));
                });
    }
}
