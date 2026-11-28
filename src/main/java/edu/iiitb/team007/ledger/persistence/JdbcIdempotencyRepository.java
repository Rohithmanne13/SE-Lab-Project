package edu.iiitb.team007.ledger.persistence;

import edu.iiitb.team007.ledger.domain.IdempotencyRecord;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** {@link IdempotencyRepository} on SQLite with parameterised SQL (SI-1, SEC-3). */
@Repository
public class JdbcIdempotencyRepository implements IdempotencyRepository {

    private final LedgerJdbc db;

    public JdbcIdempotencyRepository(LedgerJdbc db) {
        this.db = db;
    }

    @Override
    public Optional<IdempotencyRecord> find(String key) {
        return db.template().query("SELECT idempotency_key, request_hash, transaction_id, response_status, "
                        + "response_body, created_at FROM idempotency_records WHERE idempotency_key = ?",
                (rs, row) -> new IdempotencyRecord(rs.getString(1), rs.getString(2), UUID.fromString(rs.getString(3)),
                        rs.getInt(4), rs.getString(5), rs.getString(6)), key).stream().findFirst();
    }

    @Override
    public void insert(IdempotencyRecord r) {
        db.template().update("INSERT INTO idempotency_records (idempotency_key, request_hash, transaction_id, "
                        + "response_status, response_body, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                r.key(), r.requestHash(), r.transactionId().toString(), r.responseStatus(), r.responseBody(),
                r.createdAt());
    }
}
