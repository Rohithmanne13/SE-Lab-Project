---
title: "Stage 3 review: testing and CI"
type: review
status: distilled
tags: [review, stage-3]
created: 2026-11-28
author: Manne Rohith Sai
related: [SB-2, TQ-1, TQ-2, TQ-3, TQ-4, TQ-6, TQ-7, TQ-8, FR-3, FR-5, SK-5, NFR-PERF-1, NFR-SAFE-3, "#41", "#46"]
---

# Stage 3 review: testing and CI

Written under SB-2 at the end of Stage 3. Each member adds their own points, signed with their name.

## What we delivered

- The complete API of [[srs-requirements]]: users and wallets, deposits, idempotent and concurrency-safe transfers, reversals, reconciliation, queries and health (FR-0 to FR-6), built on one `BEGIN IMMEDIATE` unit of work per money-moving request as [[sqlite-transactions]] describes, with the hash chain of ADR-0003. (Rohith)
- Unit, integration, system, negative, precision and security tests for TQ-1 to TQ-6 and TQ-SEC, each system test ending with a reconciliation that must report PASSED (NFR-REL-1). (Rohith)
- The test plan and CI results, a coverage gate, the OpenAPI contract with Swagger UI, the simulate-transfer skill, load and crash tests, the usage guide and the Postman collection; the defect log and the skill evaluations (TQ-7, TQ-8). (Suhith)

## What worked

- Running every deposit, transfer and reversal in one write transaction made the FR-3.4 and FR-3.5 acceptance tests deterministic: exactly 10 of 20 and 1 of 2 transfers succeed on every run. (Rohith)
- Checking reconciliation after every system test turned FR-5 into a safety net for the whole suite: a test that left the books unbalanced would fail at once. (Rohith)
- The OpenAPI contract test compares the YAML with the running code's endpoints and records, so the documentation cannot drift from the API. (Suhith)
- The crash test killed the service three times under load and no acknowledged transfer was lost, which shows NFR-SAFE-3 and NFR-SAFE-4 on the real jar, not only in unit tests. (Suhith)

## What did not work

- The first strict-JSON setting stopped the service from starting at all - a setting with the right name but the wrong type (defect D1, #48). (Rohith)
- Our first test base class let the shared test database override a test's own database, so the amount-limit test pushed the shared system accounts to the 2^53 - 1 limit and unrelated tests failed (defect D3, #50). (Rohith)
- The first load test measured a p95 of 241.6 ms for POST /transfers, close to the 250 ms limit, because each request opened a new SQLite connection; pooling the connections brought it to about 150 ms (defect D5, #52). (Suhith)
- Test databases were first created inside the repository folder, which on our WSL clones is the Windows drive - slow, and not the local disk SQLite expects (defect D4, #51). (Suhith)

## What to change in Stage 4

- Rehearse the Stage 4 demonstration on the demo laptop with the Postman collection and simulate-transfer, with the keys set from the environment. (Rohith)
- Run the load and crash tests on the demo machine before the review, since their timings depend on the machine. (Suhith)
