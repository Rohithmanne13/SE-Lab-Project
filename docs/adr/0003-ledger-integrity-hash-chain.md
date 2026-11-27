# ADR-0003: Ledger integrity - append-only tables and the hash chain

## Status

Accepted (2026-11-23)

## Context

- Ledger records can never be updated or deleted; corrections are explicit reversal transactions (FR-4.1, FR-4.5, BR-4, DR-4).
- The database itself must enforce immutability, e.g. with triggers (FR-4.2).
- Each entry records a strictly increasing `sequence`, `prev_hash` and `entry_hash`; `entry_hash` is the SHA-256 of `prev_hash` and the entry's fields "in a fixed, documented serialisation"; the first `prev_hash` is 64 zeros (FR-4.3, FR-4.4).
- Reconciliation recomputes the chain in sequence order and verifies every hash and that sequence numbers have no gaps (FR-5.5); a tamper test must turn the report to FAILED (SRS Appendix D).
- FR-4.7 shows `reversed_by_transaction_id` on a reversed transaction - but the original row may never be updated.

## Decision

1. **Append-only by trigger.** The schema (`docs/design/data-model.md`) defines `BEFORE UPDATE` and `BEFORE DELETE` triggers on `transactions` and `ledger_entries` that execute `SELECT RAISE(ABORT, '<table> is append-only')`. No code path issues `UPDATE`/`DELETE` on them; the API has no such operation (FR-4.1).
2. **Sequence.** `sequence` is assigned inside the write transaction as `MAX(sequence) + 1` (1 for the first entry); because writers are serialised (ADR-0002) the numbers are gap-free and strictly increasing. A `UNIQUE` constraint backs this up.
3. **Hash serialisation (FR-4.4).** For each entry:

   ```text
   entry_hash = lowercase_hex( SHA-256( UTF-8( prev_hash + "\n" +
                sequence + "|" + entry_id + "|" + transaction_id + "|" + wallet_id + "|" +
                direction + "|" + amount + "|" + currency + "|" + created_at ) ) )
   ```

   - `sequence` and `amount`: base-10 integers, no sign, no leading zeros;
   - `entry_id`, `transaction_id`, `wallet_id`: canonical lowercase UUID text (DR-5);
   - `direction`: `DEBIT` or `CREDIT`; `currency`: upper-case ISO 4217 code;
   - `created_at`: RFC 3339 UTC with milliseconds, e.g. `2026-11-23T10:15:30.123Z` (the stored value, byte for byte);
   - `prev_hash`: the `entry_hash` of the entry with `sequence - 1`, or 64 `0` characters for `sequence = 1`.
4. **Entries of one transaction** are inserted in a fixed order (DEBIT entries first, then CREDIT entries, each in wallet order), so the chain is reproducible.
5. **Reversals (FR-4.5-FR-4.7).** A REVERSAL transaction stores `reverses_transaction_id` (unique, so a transaction is reversed at most once). `reversed_by_transaction_id` is **derived** when reading a transaction (`SELECT transaction_id FROM transactions WHERE reverses_transaction_id = ?`), never written into the original row.
6. **Verification (FR-5.5).** `ReconciliationService` reads all entries in `sequence` order in one read transaction, recomputes every `entry_hash` with the same serialiser, and reports the first mismatch, any `prev_hash` break and any sequence gap as discrepancies.

## Alternatives considered

- **Hashing a JSON document per entry** - JSON key order and number formatting vary between libraries; a pipe-joined line of fixed fields is unambiguous because none of the fields can contain `|` or a newline.
- **Per-wallet hash chains** - more parallel, but the SRS defines one global `sequence` and "the preceding sequence number" (FR-4.4).
- **Updating the original transaction with `reversed_by_transaction_id`** - would require an `UPDATE` on an append-only table, contradicting FR-4.2.
- **Revoking write privileges instead of triggers** - SQLite has no user privileges; triggers are the database-level mechanism.

## Consequences

- A change made directly in the database file (with triggers dropped) is detected by the next reconciliation (tamper test, Stage 3).
- The serialiser is the single place that defines the format; unit tests pin it with a known vector (TQ-1).
- Inserting entries costs one extra indexed read (`MAX(sequence)`) per transaction - negligible at our scale.

## Sources

- [[project-statement-financial-ledger]] - FR-4 (immutable audit trail, reversals) and FR-5 (reconciliation) as set by the course.
- [[sqlite-transactions]] - serialised writers, which make the gap-free sequence possible.
- [[srs-requirements]] - FR-4.1-FR-4.7, FR-5.5, DR-4, BR-4.
