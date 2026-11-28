package edu.iiitb.team007.ledger.persistence;

import edu.iiitb.team007.ledger.domain.IdempotencyRecord;
import java.util.Optional;

/**
 * Persistence of idempotency records (FR-2.5, FR-2.9). One table serves every idempotent endpoint, so a key is
 * unique across all of them; records are never deleted (no expiry in release 1.0).
 */
public interface IdempotencyRepository {

    Optional<IdempotencyRecord> find(String key);

    void insert(IdempotencyRecord idempotencyRecord);
}
