---
title: "Project statement: Financial Ledger & Digital Wallet API"
type: research
status: inbox
tags: [course, requirements, ledger]
created: 2026-11-20
author: Manne Rohith Sai
related: [FR-1, FR-2, FR-3, FR-4, FR-5, SK-5, TQ-1, TQ-2, TQ-3, TQ-4, TQ-5, TQ-6, TQ-SEC, CON-1, CON-2, "#4", "PR #18"]
---

# Project statement: Financial Ledger & Digital Wallet API

## Summary

The course's Project 2 statement. It frames the problem ("the vanishing money glitch": concurrent balance updates overwrite each other and a network retry can double-charge) and asks for a backend that exposes a RESTful API for wallets and transfers on top of a tamper-evident double-entry ledger. The stated focus is ACID transactions, data integrity, idempotency and clean API design.

## Key points

- Five functional requirements: FR-1 wallet initialization (distinct currency wallets, every balance backed by double-entry entries), FR-2 idempotent transfers (idempotency key in a header), FR-3 concurrent balance mutability without races, FR-4 immutable audit trail (corrections only as explicit reversal transactions), FR-5 ledger reconciliation endpoint (credits = debits; balances match ledger sums).
- Product skill `simulate-transfer`: create two test wallets, record initial balances, transfer with an idempotency key, fire two concurrent transfers to test race prevention, call reconciliation and summarise final balances.
- Testing requirements TQ-1 to TQ-6: unit tests (accounting, ledger assertions, idempotency-key hashing), integration tests (transactions and rollback on simulated failures), system tests for FR-1 to FR-5, negative tests (negative balance, insufficient funds, invalid idempotency payloads, malformed IDs, concurrent transfers), CI on an isolated test database on every push, and no floating-point money (integer minor units or exact decimals).
- Security testing: validate all API parameters; prevent negative amounts being used to inflate balances.
- Database guidance: SQLite is strongly encouraged (serverless single file, in-memory for unit tests); it provides BEGIN/COMMIT/ROLLBACK; WAL mode (`PRAGMA journal_mode=WAL;`) or simple transaction locks can satisfy FR-3 without distributed lock servers.

## Links

- Our refinement of these requirements is in [[srs-requirements]] (features 4.1-4.5 map one-to-one to FR-1-FR-5).

## Requirements

- [[srs-requirements#FR-1|FR-1]]
- [[srs-requirements#FR-2|FR-2]]
- [[srs-requirements#FR-3|FR-3]]
- [[srs-requirements#FR-4|FR-4]]
- [[srs-requirements#FR-5|FR-5]]
- [[srs-requirements#SK-5|SK-5]]
- [[srs-requirements#TQ-1|TQ-1]]
- [[srs-requirements#TQ-2|TQ-2]]
- [[srs-requirements#TQ-3|TQ-3]]
- [[srs-requirements#TQ-4|TQ-4]]
- [[srs-requirements#TQ-5|TQ-5]]
- [[srs-requirements#TQ-6|TQ-6]]
- [[srs-requirements#TQ-SEC|TQ-SEC]]
- [[srs-requirements#CON-1|CON-1]]
- [[srs-requirements#CON-2|CON-2]]

Issues / PRs: #4, PR #18

## Source

- docs/course/Project_2_Financial_Ledger.pdf
- Ingested 2026-11-20 by Manne Rohith Sai with the brain-ingest skill.
