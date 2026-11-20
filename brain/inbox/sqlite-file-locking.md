---
title: "SQLite file locking and concurrency"
type: research
status: inbox
tags: [sqlite, locking, durability]
created: 2026-11-20
author: Manne Rohith Sai
related: [FR-3.6, AS-1, NFR-SAFE-3, NFR-SAFE-4, NFR-REL-2, "#4", "PR #18"]
---

# SQLite file locking and concurrency

## Summary

SQLite's description of file locking and concurrency in version 3 (rollback-journal mode): the five lock states, how a writer acquires them, hot journals and crash recovery, and the conditions under which a database can be corrupted.

## Key points

- Lock states: UNLOCKED; SHARED (any number of readers; nobody may write while SHARED locks are active); RESERVED (one process plans to write; new SHARED locks still allowed; only one RESERVED lock at a time); PENDING (waiting for SHARED locks to clear; no new SHARED locks allowed); EXCLUSIVE (needed to write the file; only one, no other locks).
- A writer first gets SHARED, then RESERVED; if it cannot get RESERVED because another process holds it, the write fails with SQLITE_BUSY. It records original page content in a rollback journal before changing pages, then obtains PENDING and EXCLUSIVE to write the database file.
- A "hot" journal (left by a crash or power failure mid-transaction) is rolled back before the database is next read, which restores the last committed state.
- Corruption risks named by the documentation include file locking that does not work correctly on many network filesystems (NFS, and reported problems on Windows network filesystems) - "your best defense is to not use SQLite for files on a network filesystem" - and fsync()/FlushFileBuffers() that do not really flush data to disk.

## Links

- Complements [[sqlite-transactions]] and [[sqlite-write-ahead-logging]].
- Team observation (not from the source): hot-journal rollback is the mechanism behind NFR-REL-2 (recover by restarting) and the all-or-nothing guarantee in NFR-SAFE-3; the network-filesystem warning is why the SRS assumes a local disk (AS-1).

## Requirements

- [[srs-requirements#FR-3.6|FR-3.6]]
- [[srs-requirements#AS-1|AS-1]]
- [[srs-requirements#NFR-SAFE-3|NFR-SAFE-3]]
- [[srs-requirements#NFR-SAFE-4|NFR-SAFE-4]]
- [[srs-requirements#NFR-REL-2|NFR-REL-2]]

Issues / PRs: #4, PR #18

## Source

- https://www.sqlite.org/lockingv3.html
- Ingested 2026-11-20 by Manne Rohith Sai with the brain-ingest skill.
