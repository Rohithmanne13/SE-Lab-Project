---
title: "SQLite transactions"
type: research
status: inbox
tags: [sqlite, transactions, concurrency]
created: 2026-11-20
author: Manne Rohith Sai
related: [FR-3, FR-3.1, FR-3.2, FR-3.6, SI-3, CON-2, NFR-SAFE-3]
---

# SQLite transactions

## Summary

SQLite's documentation of transaction control (BEGIN, COMMIT, ROLLBACK). It explains read versus write transactions, the DEFERRED, IMMEDIATE and EXCLUSIVE transaction types, and how errors inside a transaction are handled. This is the mechanism our ledger relies on for atomic, serialised money movements.

## Key points

- No reads or writes happen outside a transaction; any SQL command automatically starts one if none is active, and automatic transactions commit when the last statement finishes. BEGIN...COMMIT transactions do not nest (SAVEPOINT is used for nesting).
- SQLite allows many simultaneous read transactions but only one write transaction at a time.
- A read transaction sees a historic snapshot: changes committed by other connections are not visible to it until it ends.
- If a read transaction tries to upgrade to a write while another connection is writing, the write statement fails with SQLITE_BUSY.
- DEFERRED (the default) starts the transaction only when the database is first accessed; IMMEDIATE starts a write transaction immediately, and `BEGIN IMMEDIATE` may fail with SQLITE_BUSY if another write transaction is active; EXCLUSIVE equals IMMEDIATE in WAL mode.
- Errors such as SQLITE_FULL, SQLITE_IOERR, SQLITE_INTERRUPT and SQLITE_NOMEM may roll back only the current statement or the whole transaction; the documentation suggests an application may issue an explicit ROLLBACK so the state is always the same afterwards.

## Links

- The project statement points to SQLite transactions as the basis for double-entry bookkeeping: [[project-statement-financial-ledger]].
- Team observation (not from the source): starting every money-moving operation with `BEGIN IMMEDIATE` takes the write lock before the balance is read, which is what FR-3.1 and FR-3.2 require; a lock that cannot be obtained in time maps to FR-3.6 (503 SERVICE_BUSY).

## Requirements

- [[srs-requirements#FR-3|FR-3]]
- [[srs-requirements#FR-3.1|FR-3.1]]
- [[srs-requirements#FR-3.2|FR-3.2]]
- [[srs-requirements#FR-3.6|FR-3.6]]
- [[srs-requirements#SI-3|SI-3]]
- [[srs-requirements#CON-2|CON-2]]
- [[srs-requirements#NFR-SAFE-3|NFR-SAFE-3]]

## Source

- https://www.sqlite.org/lang_transaction.html
- Ingested 2026-11-20 by Manne Rohith Sai with the brain-ingest skill.
