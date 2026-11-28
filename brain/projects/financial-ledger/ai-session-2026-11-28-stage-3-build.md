---
title: "AI session 2026-11-28: preparing the Stage 3 deliverables"
type: ai-session
status: distilled
tags: [ai-session, stage-3, testing, ci]
created: 2026-11-28
author: Manne Rohith Sai
related: [FR-0, FR-1, FR-2, FR-3, FR-4, FR-5, FR-6, TQ-1, TQ-2, TQ-3, TQ-4, TQ-5, TQ-6, TQ-7, TQ-8, SK-5, AI-1, AI-2, AI-3, "#37", "#46"]
---

# AI session 2026-11-28: preparing the Stage 3 deliverables

Recorded under AI-3. Tool: Claude Code (model Claude Opus 5.5). Session run by Manne Rohith Sai; the work was then committed and pushed in pull requests by Manne Rohith Sai and Sunnapu Suhith Reddy, each reviewed by the other member (AI-1).

## Goal

Prepare every Stage 3 deliverable of [[sdlc-and-workflow]] strictly within [[srs-requirements]]: the full API (FR-0 to FR-6) with the tests of TQ-1 to TQ-8 and TQ-SEC, the continuous testing and CI plan and results, the simulate-transfer skill with results, the defect log with regression tests and the Stage 3 review note - split into ten pull requests, six of them Suhith's and four Rohith's, in a fixed order.

## What worked

- Building each pull request in order on a scratch copy and running every check after each one: the full Java suite, the Python tests, the validators and the real skills.
- Testing through real HTTP on a random port against a real SQLite file, so the concurrency tests exercise `BEGIN IMMEDIATE` and the busy timeout exactly as in production ([[sqlite-transactions]]).
- Writing the hash chain twice - in Java and independently in Python for the load test - and letting reconciliation compare them over 100 000 entries.
- Validating `openapi.yaml` with an independent OpenAPI 3.1 validator and executing every request of the Postman collection against the running service.

## What failed or needed correction

- Five product or test defects, each logged with an Issue and a regression test in `docs/testing/defect-log.md`: a Jackson setting of the wrong type that stopped the service from starting, local-check without API keys, a test database override, test databases on the Windows drive, and a p95 latency close to the NFR-PERF-1 limit.
- Installing the newman command-line runner from npm was refused by the tool's safety check; the collection was instead executed with a small standard-library Python runner.
- An unquoted shell here-document let the shell run back-quoted words of a documentation text as commands; every file block in the guides is therefore written with a quoted end marker (`'TEAM007_FILE_END'`).

## Human review

Each pull request is reviewed and approved by a member other than its author before merge (AI-1, PR-3); every member must be able to explain the merged work (AI-2).
