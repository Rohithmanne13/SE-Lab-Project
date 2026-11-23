---
title: "SQLite write-ahead logging (WAL)"
type: research
status: inbox
tags: [sqlite, wal, concurrency]
created: 2026-11-20
author: Manne Rohith Sai
related: [SI-2, CON-2, FR-5.1, NFR-REL-5, AS-1, "#4", "PR #18"]
---

# SQLite write-ahead logging (WAL)

## Summary

SQLite's documentation of Write-Ahead Logging (WAL), an alternative to the default rollback journal. In WAL mode changes are appended to a separate WAL file and later checkpointed into the database, which lets readers and a writer work at the same time.

## Key points

- Advantages: usually faster; readers do not block writers and a writer does not block readers; more sequential I/O; fewer fsync() calls.
- Disadvantages: all processes must be on the same host (WAL does not work over a network filesystem, because readers share a wal-index in shared memory); multi-database transactions are atomic per database only; page size cannot change after entering WAL mode; read-only WAL databases cannot be opened without write access to the -shm file or directory.
- A COMMIT is a commit record appended to the WAL; the original database file is unchanged until a checkpoint copies WAL content back. By default SQLite checkpoints automatically when the WAL reaches 1000 pages.
- Each reader remembers an "end mark" when its read starts and sees the database as of that point for its whole transaction (a consistent snapshot).
- There is only one WAL file, so there can only be one writer at a time.
- WAL is enabled with `PRAGMA journal_mode=WAL;` (it returns "wal" on success) and, unlike other journal modes, it is persistent across reopenings of the database.
- Some obscure cases can still return SQLITE_BUSY in WAL mode, so applications should be prepared for it.

## Links

- Builds on transaction behaviour in [[sqlite-transactions]].
- Team observation (not from the source): the snapshot behaviour supports FR-5.1 (reconciliation reads one consistent snapshot while transfers continue) and NFR-REL-5; the same-host restriction matches assumption AS-1.

## Requirements

- [[srs-requirements#SI-2|SI-2]]
- [[srs-requirements#CON-2|CON-2]]
- [[srs-requirements#FR-5.1|FR-5.1]]
- [[srs-requirements#NFR-REL-5|NFR-REL-5]]
- [[srs-requirements#AS-1|AS-1]]

Issues / PRs: #4, PR #18

## Source

- https://www.sqlite.org/wal.html
- Ingested 2026-11-20 by Manne Rohith Sai with the brain-ingest skill.
