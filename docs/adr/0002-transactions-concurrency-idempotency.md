# ADR-0002: Transactions, concurrency and idempotency on SQLite

## Status

Accepted (2026-11-23)

## Context

- Every balance-changing operation (deposit, transfer, reversal) must run its balance check, entry inserts, balance updates and idempotency record in **one** write transaction that holds the write lock from its first read to its commit, so no write can interleave (FR-3.1, FR-3.2, FR-3.3, SI-3).
- Concurrency must come from SQLite transactions and locking alone - WAL and/or `BEGIN IMMEDIATE` with a busy timeout, no lock server (CON-2, CI-5).
- A lock not obtained within the busy timeout (>= 5000 ms) fails with 503 `SERVICE_BUSY` and `Retry-After`, persisting nothing (FR-3.6).
- Idempotency: key required, request fingerprint = SHA-256 of method, path and canonical body; record written in the same transaction as the entries; same key + same fingerprint -> stored response replayed with `Idempotent-Replayed: true`; same key + different fingerprint -> 422; failures store nothing; of concurrent requests with one key at most one posts and later ones get the replay; keys kept forever (FR-2.3-FR-2.9).
- Durability: a 2xx only after commit with `synchronous = FULL`; atomic under crashes (NFR-SAFE-3, NFR-SAFE-4).
- Reconciliation reads one consistent snapshot while transfers continue (FR-5.1, NFR-REL-5).

## Decision

1. **Connection settings (SI-2).** Every connection is opened by one `DataSource` factory with: `foreign_keys = ON`, `journal_mode = WAL` (file databases), `synchronous = FULL`, `busy_timeout = 5000`, and sqlite-jdbc's `transaction_mode = IMMEDIATE`, so every transaction the application starts begins with `BEGIN IMMEDIATE`.
2. **One unit of work per money-moving request.** A `TransactionRunner` in the persistence layer opens a connection, starts the transaction (`BEGIN IMMEDIATE`, i.e. takes the single write lock before the first read), runs the service callback, and commits; any exception rolls back. Services never read a balance outside this callback (FR-3.2).
3. **Order inside the unit of work** (deposit, transfer and reversal alike):
   1. look up the idempotency key; if present, compare fingerprints -> equal: return the stored status and body (no writes) / different: 422 `IDEMPOTENCY_KEY_REUSED`;
   2. load and validate the wallets (existence, currency, not the same wallet, not a system wallet for CLIENT calls);
   3. check funds against the balance read in this transaction (422 `INSUFFICIENT_FUNDS`) and the range limit (422 `AMOUNT_LIMIT_EXCEEDED`);
   4. insert the transaction and its entries (hash-chained, ADR-0003), update both stored balances;
   5. assert the transaction is balanced (sum DEBIT = sum CREDIT, FR-1.8) - otherwise throw, which rolls everything back;
   6. insert the idempotency record with the response status and body;
   7. commit; only then is the 201 sent (NFR-SAFE-4).
4. **Busy database.** `SQLITE_BUSY` after the 5000 ms busy timeout is mapped to 503 `SERVICE_BUSY` with `Retry-After: 1` (FR-3.6); nothing was written because the transaction never started or was rolled back.
5. **Concurrent requests with the same key** are serialised by the write lock: the first commits its record, and every later one finds that record at step 3.1 and receives the replay (FR-2.8). We deliberately do **not** answer 409 for an in-flight duplicate as the IETF draft suggests - with `BEGIN IMMEDIATE` a duplicate can never observe an uncommitted original, so the SRS behaviour is always reachable.
6. **Fingerprint (FR-2.4).** SHA-256 (lowercase hex) of the UTF-8 bytes of `METHOD + "\n" + path + "\n" + canonical body`, where the canonical body is the parsed JSON re-serialised with object keys sorted at every level and no insignificant whitespace (Jackson with `ORDER_MAP_ENTRIES_BY_KEYS`). The path is the request path without query string.
7. **Key format** follows the SRS exactly: 8-128 characters of `A-Z a-z 0-9 - _` (header section 3.3.3). A value sent in the draft's quoted Structured-Field form (`"abc..."`) is malformed under the SRS and gets 400 `IDEMPOTENCY_KEY_INVALID`.
8. **Reads.** Queries and reconciliation use ordinary read transactions; in WAL mode each sees the last committed state and never blocks the writer (FR-6.5, FR-5.1, NFR-REL-5).

## Alternatives considered

- **Optimistic locking with a version column** - needs retry loops and still risks starvation; the SRS asks for serialisable execution, which SQLite's single writer gives for free.
- **Application-level locks (Java `synchronized`, per-wallet locks)** - break as soon as two processes share the file and duplicate what the database already guarantees (CON-2).
- **Reserving idempotency keys in a separate earlier transaction** - would need a "pending" state and cleanup after crashes; one transaction keeps record and entries atomic (FR-2.5).
- **Answering 409 for in-flight duplicates (IETF draft)** - unnecessary under `BEGIN IMMEDIATE` and contrary to FR-2.8.

## Consequences

- Writers are strictly one at a time; at our scale (NFR-PERF-2: >= 50 transfers/s) short transactions make this sufficient, and long reads never block them under WAL.
- The FR-3.4 and FR-3.5 acceptance tests (20 x 100 from 1000; 2 x 600 from 1000) are deterministic: exactly the requests that fit succeed.
- Tests use a fresh database file per test, because WAL and the busy timeout behave like production only on a file (TQ-5); a separate test forces `SQLITE_BUSY` to prove FR-3.6.
- Stage 3 integration tests inject a fault between the DEBIT insert and the commit to prove the rollback (TQ-2, NFR-SAFE-3).

## Sources

- [[sqlite-transactions]] - one writer at a time, `BEGIN IMMEDIATE` takes the write lock immediately and may fail with `SQLITE_BUSY`, explicit `ROLLBACK` on error.
- [[sqlite-write-ahead-logging]] - readers and the writer run concurrently, each reader sees one snapshot, WAL is persistent, same host only.
- [[sqlite-file-locking]] - rollback-journal recovery after a crash restores the last committed state.
- [[idempotency-key-header-draft]] - key semantics, fingerprints, 400/422 cases, and the 409 suggestion we deliberately do not follow.
- [[srs-requirements]] - FR-2, FR-3, SI-2, SI-3, CON-2, NFR-SAFE-3, NFR-SAFE-4.
