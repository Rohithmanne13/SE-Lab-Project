---
title: "SRS requirement index"
type: concept
status: distilled
tags: [srs, requirements, traceability]
created: 2026-11-20
author: Manne Rohith Sai
related: [SE-1, SE-5, SB-1, "#1"]
---

# SRS requirement index

Every requirement ID of the submitted SRS (`docs/srs/Team007_SRS_Submission.pdf`, Stage 1) as a heading, with its statement copied verbatim from the SRS.
It exists so that notes, ADRs and skills can link to an exact requirement, e.g. `[[srs-requirements#FR-2.4]]` (SB-1).
The SRS is frozen: if this note and the SRS ever differ, the SRS wins. Do not edit statements here.

## 2.4 Operating Environment

### OE-1

The service shall run on Windows 10/11, macOS 13+ and Ubuntu 22.04 LTS+, on x86-64 or ARM64 hardware.

### OE-2

The service shall use SQLite 3.35.0 or later, in-process through a standard or widely used driver; no separate database server.

### OE-3

The service shall run as a single process bound to a configurable host and port (default 127.0.0.1:8000).

### OE-4

The CI pipeline shall run on GitHub Actions hosted runners (Ubuntu, latest LTS image).

### OE-5

The service shall coexist with the tools named in [2]: Git, GitHub, Obsidian, Postman/Bruno/cURL and AI coding assistants.

## 2.5 Design and Implementation Constraints

### CON-1

Database. SQLite shall be the system of record [1 §5]: a file database for normal runs, an in-memory or freshly created file database for automated tests (TQ-5).

### CON-2

Concurrency. FR-3 shall be met with SQLite transactions and locking (WAL mode and/or BEGIN IMMEDIATE write transactions with a busy timeout) [1 §5][5]; no distributed lock server or external cache.

### CON-3

Money. Amounts shall be stored, computed and transmitted as integers in minor units (paise, cents); binary floating-point types shall not be used for money anywhere in code or schema (TQ-6).

### CON-4

API style. RESTful JSON over HTTP/1.1 [6][7], versioned under /api/v1, documented in OpenAPI 3.x [14] (SE-7).

### CON-5

Architecture. Layered code (API → service/domain → persistence); database access, clock and ID generation isolated behind provider interfaces so tests can substitute them (SE-6).

### CON-6

Language. One mainstream language with a maintained HTTP framework, test framework, linter and SQLite driver. The concrete choice is a design decision recorded in an ADR in docs/adr/ in Stage 2 (SE-3); it does not alter any requirement here.

### CON-7

Repository. Code, tests, docs, skills and the brain vault shall live in one GitHub repository and change only through reviewed pull requests (Section 6.5).

### CON-8

Skill format. Every skill shall be a folder containing a SKILL.md conforming to the Agent Skills format [15].

### CON-9

Secrets. No secret, token, API key or credential shall be committed or pasted into any AI tool [2 §6]; keys come from environment variables (an example file with dummy values may be committed).

## 2.7 Assumptions and Dependencies

### AS-1

Single service instance with exclusive access to its SQLite file on a local disk (not a network share, where SQLite locking is unreliable [5]).

### AS-2

Clients are trusted backend applications authenticating with an API key; end-user authentication is done by the client application.

### AS-3

Money enters only through administrative deposits representing externally verified funds; no real payment network is connected.

### AS-4

The host clock is reasonably accurate; timestamps are informational and ordering is defined by the ledger sequence number.

### AS-5

Expected load is that of an academic demonstration (Section 5.1), not production banking volumes.

### AS-6

Every member has GitHub, Git, the language runtime and Obsidian (local use, no online account) installed.

### DE-1

SQLite (public domain) for storage, transactions and locking.

### DE-2

GitHub (repository, Issues, pull requests, releases) and GitHub Actions (CI).

### DE-3

An HTTP framework, test framework, linter, OpenAPI tooling and secret scanner for the chosen language (recorded in an ADR, CON-6).

### DE-4

Obsidian for viewing and editing the brain/ vault; AI coding assistants able to execute Agent Skills.

## 3.1 User Interfaces

### UI-1

The service shall publish interactive API documentation generated from the OpenAPI file (e.g. Swagger UI at /docs) in development mode.

### UI-2

A Postman or Bruno collection covering every endpoint shall be provided for manual testing and the Stage 4 demonstration.

### UI-3

Every error response shall be human- and machine-readable: a stable error code, a one-sentence detail telling the caller what to correct and, for validation errors, the offending fields (3.3.4).

### UI-4

Agent Skills shall print a concise, human-readable report marking each step PASS or FAIL.

## 3.3.1 Database Interface

### SI-1

The service shall access SQLite only through the persistence provider interface (CON-5) using parameterised SQL.

### SI-2

On connect it shall set PRAGMA foreign_keys = ON, journal_mode = WAL (file databases), synchronous = FULL and a busy timeout ≥ 5000 ms.

### SI-3

Every operation writing money-related data shall run in one write transaction started with BEGIN IMMEDIATE and ended with COMMIT or ROLLBACK [5].

### SI-4

The schema shall be created and evolved by versioned migration scripts in the repository, applied at start-up.

## 3.4 Communications Interfaces

### CI-1

HTTP/1.1 [6]; localhost in development. Any deployment beyond localhost shall sit behind TLS 1.2+ (HTTPS) so keys and financial data never travel in clear text.

### CI-2

Encoding shall be UTF-8 JSON [7]; monetary amounts shall be JSON integers (no quotes, fraction or exponent).

### CI-3

A successful resource-creating POST shall return 201 with a Location header pointing to the new resource.

### CI-4

A replayed idempotent response shall carry the header Idempotent-Replayed: true.

### CI-5

Synchronisation between concurrent requests is done solely by database transactions (CON-2); the API is stateless between requests.

## 4 System Features

### FR-1

**4.1 Wallet Initialization.** Create user accounts owning distinct per-currency wallets. Every balance is backed by double-entry ledger entries: a wallet starts at zero and money enters it only through a balanced transaction (deposit, transfer or reversal). Priority: High (Benefit 9, Penalty 9, Cost 3, Risk 3; scale 1–9).

#### FR-1.1

The system shall create a user from name (1–100 chars after trimming) and email (syntactically valid, ≤ 254 chars), assign a UUID user_id and return 201 with user_id, name, email, created_at.

#### FR-1.2

Emails shall be case-insensitive; a duplicate shall be rejected with 409 EMAIL_ALREADY_EXISTS.

#### FR-1.3

The system shall create a wallet for an existing user from currency (uppercase three-letter ISO 4217 code [12] in the configured supported set, at least INR and USD), returning 201 with wallet_id, user_id, currency, balance (= 0), created_at.

#### FR-1.4

At most one wallet per user per currency (duplicate → 409 WALLET_ALREADY_EXISTS); a user may hold wallets in several currencies.

#### FR-1.5

Per supported currency the system shall keep exactly one system funding account (wallet flagged is_system, owned by an internal system user) representing money entering from outside. These are created automatically at start-up, are the only wallets allowed a negative balance, and are never returned or accepted by CLIENT endpoints.

#### FR-1.6

On POST /wallets/{walletId}/deposits (ADMIN, Idempotency-Key required) with a positive integer amount, the wallet’s currency and optional description (≤ 255 chars), the system shall in one ACID transaction create a DEPOSIT transaction — DEBIT on that currency’s system funding account, CREDIT of the same amount on the wallet — update both balances and return 201 with the transaction. FR-2.3–FR-2.8 apply.

#### FR-1.7

A balance shall never be set or changed directly through the API; every change results from ledger entries, so a wallet’s stored balance always equals Σ CREDIT − Σ DEBIT of its entries.

#### FR-1.8

Every ledger transaction shall have at least two entries in a single currency with total DEBIT = total CREDIT; the system shall refuse to commit any violating transaction (500 INTERNAL_ERROR, nothing persisted).

#### FR-1.9

Invalid input shall be rejected without side effects: malformed ID → 400 INVALID_ID; unknown user/wallet → 404; invalid field → 400 VALIDATION_ERROR; well-formed but unsupported currency → 422 UNSUPPORTED_CURRENCY.

### FR-2

**4.2 Idempotent Transfers.** Transfer funds between two wallets. The endpoint requires an idempotency key header so that a retried network request never double-charges an account [1][9]. Priority: High (Benefit 9, Penalty 9, Cost 5, Risk 6; scale 1–9).

#### FR-2.1

The system shall accept a transfer with source_wallet_id and destination_wallet_id (UUIDs of existing non-system wallets), amount (integer minor units, 1 ≤ amount ≤ 9 007 199 254 740 991), currency (equal to both wallets’ currency) and optional description (≤ 255 chars).

#### FR-2.2

On success the system shall, in one ACID transaction, create a TRANSFER transaction with exactly two entries — DEBIT amount on the source, CREDIT amount on the destination — update both balances, and return 201 with transaction_id, type, status (= COMPLETED), source_wallet_id, destination_wallet_id, amount, currency, description, created_at and the entries.

#### FR-2.3

Every POST /transfers shall carry an Idempotency-Key (format in 3.3.3); missing → 400 IDEMPOTENCY_KEY_MISSING, malformed → 400 IDEMPOTENCY_KEY_INVALID, both before any database write.

#### FR-2.4

The system shall compute a request fingerprint = SHA-256 [13] of the HTTP method, the request path and the canonical JSON body (keys sorted, insignificant whitespace removed) and store it with the key.

#### FR-2.5

The idempotency record (key, fingerprint, resulting transaction, response status and body) shall be written in the same database transaction as the ledger entries: both commit or neither does.

#### FR-2.6

A request with an already recorded key and identical fingerprint shall create no ledger entry and shall return the stored response (same status and body) with Idempotent-Replayed: true, however many times it is repeated.

#### FR-2.7

A request with an already recorded key but a different fingerprint shall return 422 IDEMPOTENCY_KEY_REUSED and change no data.

#### FR-2.8

A failed request (any 4xx/5xx) shall store no idempotency record, persist no entry and change no balance; the client may retry with the same key. Of concurrent requests carrying the same key, at most one shall post, and every request processed after that commit shall get the replayed response (FR-2.6).

#### FR-2.9

Idempotency keys shall be unique across all idempotent endpoints and retained permanently in release 1.0 (no expiry).

#### FR-2.10

The system shall reject: source = destination → 422 SAME_WALLET_TRANSFER; currency differing from either wallet → 422 CURRENCY_MISMATCH; source balance < amount → 422 INSUFFICIENT_FUNDS; amount zero, negative, fractional, in exponent notation, a string or a boolean → 400 VALIDATION_ERROR; unknown wallet → 404 WALLET_NOT_FOUND; malformed IDs → 400 INVALID_ID.

### FR-3

**4.3 Concurrent Balance Mutability.** Safely handle multiple simultaneous incoming and outgoing transfers on the same wallet without data races or balance corruption [1]. Priority: High (Benefit 9, Penalty 9, Cost 5, Risk 7; scale 1–9).

#### FR-3.1

Every balance-changing operation (deposit, transfer, reversal) shall run its balance check, entry inserts, balance updates and idempotency record inside one write transaction holding the write lock from first read to commit (BEGIN IMMEDIATE), so no other write can interleave (serialisable execution).

#### FR-3.2

The sufficient-funds check shall use the balance read inside that same transaction; a balance read outside it shall never be used to decide or compute an update (no read-modify-write across transactions).

#### FR-3.3

Under any interleaving, the final state shall equal executing the successful requests one at a time in some serial order: no lost updates, no negative non-system balance, and reconciliation (FR-5) passes.

#### FR-3.4

Acceptance: a wallet holding 1000 and 20 concurrent transfers of 100 from it → exactly 10 succeed (201), exactly 10 fail with 422 INSUFFICIENT_FUNDS, final balance 0, destinations received exactly 1000 in total.

#### FR-3.5

Acceptance: two concurrent transfers of 600 from a wallet holding 1000 → exactly one succeeds; final source balance 400.

#### FR-3.6

If the write lock is not obtained within the busy timeout (≥ 5000 ms), the request shall fail with 503 SERVICE_BUSY and Retry-After, persisting nothing.

### FR-4

**4.4 Immutable Audit Trail.** Ledger records cannot be updated or deleted once written; corrections or cancellations are recorded only as explicit reversal transactions [1]. The ledger is tamper-evident: each entry carries a SHA-256 hash chaining it to the previous entry. Priority: High (Benefit 8, Penalty 9, Cost 4, Risk 4; scale 1–9).

#### FR-4.1

The API shall expose no operation that updates or deletes a transaction or ledger entry.

#### FR-4.2

The database shall enforce immutability: triggers (or equivalent) shall abort every UPDATE or DELETE on the transaction and ledger-entry tables.

#### FR-4.3

Each ledger entry shall record entry_id, a strictly increasing integer sequence, transaction_id, wallet_id, direction (DEBIT/CREDIT), amount (> 0), currency, created_at, prev_hash and entry_hash.

#### FR-4.4

entry_hash shall be the lowercase hex SHA-256 of prev_hash concatenated with the entry’s sequence, entry_id, transaction_id, wallet_id, direction, amount, currency and created_at in a fixed, documented serialisation; prev_hash is the entry_hash of the preceding sequence number, or 64 zeros for the first entry.

#### FR-4.5

On POST /transactions/{transactionId}/reversal (ADMIN, Idempotency-Key required, body {reason: 1–255 chars}) the system shall, in one ACID transaction, create a REVERSAL transaction referencing the original via reverses_transaction_id and containing, for every original entry, an entry with the same wallet and amount and opposite direction, update the balances and return 201. The original transaction and entries remain unchanged.

#### FR-4.6

A transaction shall be reversed at most once (409 TRANSACTION_ALREADY_REVERSED); a REVERSAL is not reversible (422 TRANSACTION_NOT_REVERSIBLE); a reversal making a non-system wallet negative → 422 INSUFFICIENT_FUNDS. FR-2.3–FR-2.8 apply.

#### FR-4.7

GET /transactions/{transactionId} shall show reversed_by_transaction_id for a reversed transaction and reverses_transaction_id for a reversal.

### FR-5

**4.5 Ledger Reconciliation.** An administrative endpoint that scans ledger entries and verifies that Σ credits = Σ debits and that current wallet balances match the calculated ledger sums [1]; it also verifies the hash chain. Priority: High (Benefit 9, Penalty 8, Cost 3, Risk 3; scale 1–9).

#### FR-5.1

Reconciliation shall be read-only and shall read all data in a single read transaction, so the report reflects one consistent snapshot even while transfers continue.

#### FR-5.2

It shall verify, per currency and in total, that Σ DEBIT = Σ CREDIT over all ledger entries.

#### FR-5.3

It shall verify that every transaction is balanced and has at least two entries in a single currency.

#### FR-5.4

It shall verify for every wallet that stored balance = Σ CREDIT − Σ DEBIT of its entries, and that no non-system wallet is negative.

#### FR-5.5

It shall recompute the hash chain in sequence order, verifying every prev_hash and entry_hash and that sequence numbers have no gaps.

#### FR-5.6

The report shall contain status (PASSED only if every check passes, else FAILED), checked_at, transactions_checked, entries_checked, wallets_checked, per-currency total_debits/total_credits, and discrepancies (each: check, affected wallet/transaction/entry ID, expected and actual value). A FAILED report still returns HTTP 200, since the report is the requested result.

#### FR-5.7

Reconciliation shall be accessible only with an ADMIN key.

### FR-6

**4.6 Queries and Health.** Read-only functions needed by clients, administrators, the simulate-transfer skill (initial/final balances) and the local-check skill (read-only fetch endpoint). Priority: Medium (Benefit 7, Penalty 7, Cost 2, Risk 2; scale 1–9).

#### FR-6.1

The system shall return a user (GET /users/{userId}), a user’s wallets (GET /users/{userId}/wallets) and a wallet with its current balance (GET /wallets/{walletId}).

#### FR-6.2

GET /wallets/{walletId}/entries shall return the wallet’s entries in ascending sequence, paginated by limit (1–100, default 50) and offset (≥ 0, default 0), with the total count; invalid paging → 400 VALIDATION_ERROR.

#### FR-6.3

GET /transactions/{transactionId} shall return the transaction, its type, amount, currency, timestamps, description and all entries.

#### FR-6.4

GET /health shall need no authentication, perform no write and return 200 {"status":"ok","database":"ok","version":"<release>"} when a trivial query succeeds, else 503 with "status":"degraded".

#### FR-6.5

Reads shall observe only committed state, never a partially applied transaction.

### FR-0

**4.7 Cross-Cutting API Behaviour.** Behaviour common to every endpoint: authentication, authorisation, validation, error format and request tracing. Priority: High (Benefit 8, Penalty 8, Cost 3, Risk 3; scale 1–9).

#### FR-0.1

Every request except GET /health and the documentation pages shall be authenticated by X-API-Key, compared in constant time with the configured CLIENT and ADMIN keys; ADMIN keys may call every endpoint, CLIENT keys every non-administrative one.

#### FR-0.2

Every path/query parameter, header and body field shall be validated against the OpenAPI schema before business logic; unknown body fields shall be rejected.

#### FR-0.3

All errors shall follow the error model and codes of 3.3.4.

#### FR-0.4

Every response shall include X-Request-ID (3.3.3).

#### FR-0.5

Bodies larger than 16 KiB shall be rejected with 413 PAYLOAD_TOO_LARGE without processing.

## 5.1 Performance Requirements

### NFR-PERF-1

With 10 concurrent clients transferring between distinct wallets, p95 latency shall be ≤ 250 ms for POST /transfers and ≤ 100 ms for GET /wallets/{walletId}.

### NFR-PERF-2

The service shall sustain ≥ 50 committed transfers per second.

### NFR-PERF-3

In the FR-3.4 test (20 concurrent transfers on one wallet), every request shall complete within 10 s and none shall return 5xx.

### NFR-PERF-4

Reconciliation over 100 000 ledger entries shall complete within 10 s.

### NFR-PERF-5

The service shall be ready to serve /health within 15 s of start on an existing database.

## 5.2 Safety Requirements

### NFR-SAFE-1

No operation shall create or destroy money: after every committed transaction Σ DEBIT = Σ CREDIT, globally and per currency.

### NFR-SAFE-2

No non-system balance shall ever become negative, including under concurrency (service check plus database CHECK constraint).

### NFR-SAFE-3

Atomicity: a failure at any point of a money-moving operation (exception, process kill, power loss) shall leave all or none of its entries, balance updates and idempotency record persisted, proven by fault-injection tests (TQ-2).

### NFR-SAFE-4

Durability: a 2xx for a money-moving operation shall be sent only after its transaction has committed with synchronous = FULL.

### NFR-SAFE-5

Automated tests shall never use the production database file; they use an isolated database (TQ-5).

## 5.3 Security Requirements

### SEC-1

All API parameters (path, query, headers, body) shall be validated for type, format, length and range (Security Testing [1]).

### SEC-2

Negative, zero, fractional and non-numeric amounts shall be rejected so no caller can inflate a balance with negative values (Security Testing [1]).

### SEC-3

All SQL shall use parameterised statements; concatenating user input into SQL is prohibited.

### SEC-4

Deposit, reversal and reconciliation shall require an ADMIN key; CLIENT keys receive 403.

### SEC-5

API keys shall come from environment variables, be ≥ 32 random characters, be compared in constant time, and never be logged, returned, committed or pasted into AI tools.

### SEC-6

Error responses shall not disclose stack traces, SQL, file paths or internal identifiers of system accounts.

### SEC-7

Logs shall not contain API keys or full request bodies; they may contain request ID, method, path, status, latency and transaction ID.

### SEC-8

CI shall run secret scanning on every pull request and fail if a secret is detected.

### SEC-9

Third-party dependencies shall be pinned to exact versions (no ranges or dynamic versions such as LATEST, RELEASE or +) in the build configuration or a lock file, so every build resolves identical dependencies.

## 5.4 Software Quality Attributes

### NFR-REL-1

Correctness. Reconciliation shall report PASSED after every automated test scenario and after every simulate-transfer run.

### NFR-REL-2

Reliability. After a crash, a restart shall suffice: SQLite journal recovery restores the last committed state and reconciliation passes.

### NFR-REL-3

Precision. All money arithmetic shall be exact integer arithmetic; no rounding anywhere (TQ-6).

### NFR-REL-4

Determinism. Replaying an idempotent request any number of times shall return a byte-identical response body.

### NFR-REL-5

Availability. Transfers shall continue to be served during a long-running reconciliation (WAL readers do not block the writer).

### NFR-REL-6

Range. No balance or amount shall exceed 9 007 199 254 740 991 minor units (2^53 − 1, the largest integer exact in every JSON client); exceeding operations → 422 AMOUNT_LIMIT_EXCEEDED.

### NFR-MAINT-1

Maintainability. Code shall follow the layering of CON-5, pass the linter with zero errors and keep functions focused (enforced in review).

### NFR-MAINT-2

Testability. Automated tests shall achieve ≥ 80 % line coverage of the service/domain and persistence layers, reported by CI.

### NFR-MAINT-3

Traceability. Every requirement shall be traceable to issue, PR, test, skill and brain note (SE-5).

### NFR-PORT-1

Portability. The project shall build, test and run with the documented commands on Windows, macOS and Linux (OE-1), verified by local-check on each member’s machine.

### NFR-USE-1

Usability. A new member following the README shall run the service and tests in under 15 minutes.

### NFR-OBS-1

Observability. The service shall log one structured JSON line per request with timestamp, request ID, method, path, status and latency.

## 5.5 Business Rules

### BR-1

Every ledger transaction is balanced: total debits equal total credits.

### BR-2

Money moves only between wallets of the same currency; no currency conversion exists.

### BR-3

A non-system wallet can never be negative; only system funding accounts may be.

### BR-4

The ledger is append-only; mistakes are corrected only by a reversal transaction.

### BR-5

Only administrators may deposit funds, reverse transactions and run reconciliation.

### BR-6

A user has at most one wallet per currency.

### BR-7

A transaction can be reversed at most once; reversals cannot be reversed.

### BR-8

Amounts are positive integers in the currency’s minor unit; direction is expressed by DEBIT/CREDIT, never by a sign.

### BR-9

A money-moving request is executed at most once per idempotency key.

## 6.1 Agent Skill Requirements

### SK-0

Every skill shall be a folder skills/<skill-name>/ (location also stated in AGENTS.md) containing a SKILL.md with YAML front matter holding at least name (lowercase letters, digits, hyphens; identical to the folder name) and description (what it does and when to use it), followed by Markdown instructions [15]. Each skill shall state inputs, steps, outputs and failure handling, cite at least one distilled brain note (SB-3), and have an evaluation run before each stage submission with results recorded in the test documentation (TQ-7). CI shall validate every SKILL.md.

### SK-1

**brain-ingest** (Stage 1). Input: a source (URL, file or pasted text) plus optional tags/requirement IDs. Creates one note in brain/inbox/ with standard front matter (DR-8), summary, key points, source reference and `[[wikilinks]]` to related notes and requirement IDs; updates brain/index.md; appends a dated entry to brain/log.md; never invents content absent from the source. Acceptance: ≥ 10 research notes added by this skill in Stage 1.

### SK-2

**brain-refine** (Stage 2). Moves inbox notes to the correct PARA folder, merges duplicates, repairs broken `[[wikilinks]]`, marks distilled notes status: distilled, updates index.md and log.md. Acceptance: inbox empty (or each remaining note has a reason) and zero broken links reported.

### SK-3

**brain-query** (Stage 2). Answers a question or drafts a document (e.g. SRS section, ADR) using only vault content, citing every note used as a `[[wikilink]]`, stating explicitly when the vault lacks the answer; logs the query in log.md. Acceptance: answers to a fixed evaluation set cite the correct notes; run live in Stage 4.

### SK-4

**local-check** (Stage 2). (1) build the project, (2) run the automated test suite, (3) start the local server, (4) invoke one read-only fetch endpoint (GET /health), (5) report PASS/FAIL per step, stop the server and exit non-zero if any step fails [2 §3]. Acceptance: passes on every member’s machine; run live in Stage 4.

### SK-5

**simulate-transfer** (Stage 3). Runs a complete workflow and verifies ledger consistency [1 §3]: (1) create two test wallets (and users), funding the source by a deposit; (2) record initial balances; (3) execute a transfer with an idempotency key, resend it with the same key and verify a replay with the balance changed only once; (4) fire two concurrent transfers from the same wallet whose total exceeds its balance and verify exactly one succeeds; (5) call reconciliation and summarise initial and final balances, each step’s PASS/FAIL and the reconciliation status. Acceptance: all steps PASS and reconciliation PASSED; results recorded in Stage 3 test docs; run end-to-end in Stage 4.

## 6.2 Testing and Quality Requirements

### TQ-1

Unit tests shall cover accounting calculations, ledger balance assertions (balanced transactions; balance = Σ CREDIT − Σ DEBIT) and idempotency-key handling incl. fingerprint hashing (FR-2.4).

### TQ-2

Integration tests shall verify database transactions and rollback on simulated transfer failures: a fault injected after the DEBIT entry and before COMMIT leaves no entries, no balance change and no idempotency record.

### TQ-3

System (end-to-end) tests through the HTTP API shall exist for each of FR-1 to FR-5.

### TQ-4

Negative tests shall cover negative-balance attempts, insufficient funds, invalid idempotency payloads (missing, malformed, reused with a different body), malformed IDs, and simultaneous concurrent transfers proving race safety (FR-3.4, FR-3.5).

### TQ-5

CI tests shall run against an isolated test database (in-memory or a fresh file per run) on every push and pull request.

### TQ-6

Tests shall prove floating-point rounding cannot occur: integer minor units end to end; inputs such as 10.5, 1e3, "100" and true rejected; the sum of many small transfers (e.g. 1000 × 1) exact.

### TQ-7

We shall run skill evaluations before each stage submission and record the results in our test documentation.

### TQ-8

We shall maintain tests as the system changes; every significant defect is logged as a GitHub Issue and gets a regression test before closing.

### TQ-SEC

Security tests shall cover input validation of all API parameters and negative-amount prevention (SEC-1, SEC-2), plus authentication/authorisation failures (401/403).

## 6.3 Second Brain Requirements

### SB-0

We shall keep a brain/ Obsidian vault in the repository organised by PARA — inbox/ (unsorted), projects/ (active deadlines), areas/ (ongoing responsibilities), resources/ (reference), archive/ (inactive) — with brain/index.md as entry point and brain/log.md recording every skill run, maintained with brain-ingest, brain-refine and brain-query.

### SB-1

Notes shall link to each other with `[[wikilinks]]` and to requirement IDs, issues and PRs.

### SB-2

We shall write a short review note (what worked, what did not, what to change) at the end of each stage.

### SB-3

Every ADR and every skill shall cite at least one distilled note.

### SB-4

We shall keep an AGENTS.md at the repository root.

### SB-5

Changes to the brain, including AI-made changes, shall go through pull requests.

### SB-6

Every team member shall author notes; contributions shall be visible in commit history.

## 6.4 Software Engineering Process Requirements

### SE-1

We shall produce and maintain this SRS with FR, NFR and SK requirements.

### SE-2

We shall produce architecture and design documentation with appropriate UML and ER diagrams (Stage 2).

### SE-3

We shall record significant decisions as ADRs in docs/adr/, each citing brain notes.

### SE-4

A development plan and task breakdown shall be produced and kept up to date.

### SE-5

We shall keep a traceability matrix requirement → issue → PR → test → skill → brain note in docs/traceability.md, seeded from Appendix D.

### SE-6

External dependencies shall be isolated behind provider interfaces (CON-5).

### SE-7

We shall produce user documentation and developer API documentation in OpenAPI (Section 2.6).

## 6.5 Version Control and CI Requirements

### PR-1

Every member shall make meaningful, incremental commits; the branching strategy shall be documented in CONTRIBUTING.md.

### PR-2

All work and defects shall be tracked as GitHub Issues.

### PR-3

Every change (code, skills, brain) shall be merged through a pull request with at least one approving review by a member other than its author; branch protection shall block direct pushes to main.

### PR-4

Releases shall be tagged with Semantic Versioning and documented in CHANGELOG.md [18]; the final release is tagged for Stage 4.

### PR-5

GitHub Actions shall run on every pull request and push: build, lint, automated tests on an isolated database, secret scanning, validation of every SKILL.md (front matter, naming) and of brain notes (front-matter schema, broken links). A failing check blocks the merge.

## 6.6 AI-Assisted Development Policy

### AI-1

We may use AI coding assistants but remain responsible for every merged line; a team member shall review every AI-generated change in a PR.

### AI-2

Every member shall be able to explain any merged code, skill or document during project reviews.

### AI-3

Significant AI sessions shall be recorded in the brain as ai-session notes (goal, what worked, what failed).

### AI-4

No secret, token or credential shall be pasted into any AI tool.

## 6.7 Data Requirements

### DR-1

The logical data model shall contain USER, WALLET, TRANSACTION, LEDGER_ENTRY and IDEMPOTENCY_RECORD with the attributes and relationships of Figure 2.

### DR-2

Monetary columns shall be SQLite INTEGER; CHECK constraints shall enforce amount > 0 on entries and transactions and balance ≥ 0 for non-system wallets.

### DR-3

Foreign keys shall be enforced; UNIQUE constraints shall enforce user email (case-insensitive), (user_id, currency) on wallets, sequence on entries, idempotency_key on idempotency records and reverses_transaction_id on transactions.

### DR-4

Ledger entries and transactions shall be append-only (FR-4.2).

### DR-5

Timestamps shall be stored as RFC 3339 UTC strings and identifiers as canonical lowercase UUID text.

### DR-6

The database file location shall be configurable by environment variable; tests use a separate location or :memory:.

### DR-7

Ledger data shall be retained indefinitely; no purge function exists in release 1.0.

### DR-8

Every brain note shall begin with YAML front matter with at least title, type (research, concept, decision, ai-session, review, meeting), status (inbox, distilled, archived), tags, created (YYYY-MM-DD), author and related (requirement IDs, issues, PRs); CI validates this schema.
