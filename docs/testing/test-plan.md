# Continuous testing and CI plan

Stage 3 deliverable "continuous testing and CI plan/results" (SRS 6.8, PR-5, TQ-1 to TQ-8, TQ-SEC). It says what
we test, at which level, with which tools, where the tests run and when a change may merge. Results are in the
last section. Requirement statements: `brain/projects/financial-ledger/srs-requirements.md`.

## 1. Test levels

| Level | What it proves | Where | Requirements |
|---|---|---|---|
| Unit | Accounting calculations and ledger balance rules (balance = sum CREDIT - sum DEBIT, balanced transactions), money limits, hash chain and request fingerprint against fixed vectors, canonical JSON, paging and key rules | `src/test/java/.../domain`, `support`, `config`, `service/PostingServiceTest` (substituted repositories, fixed clock, sequential IDs) | TQ-1, FR-1.7, FR-1.8, FR-2.4, FR-4.4, SE-6 |
| Integration | Units of work on a real SQLite file: commit, rollback, BEGIN IMMEDIATE and the busy timeout, readers not blocking the writer, migrations, schema constraints and append-only triggers, repository round trips, a fault injected after the DEBIT entry | `src/test/java/.../persistence`, `api/FaultInjectionTest`, `api/BusyApiTest` | TQ-2, SI-1-SI-4, DR-2-DR-4, FR-3.6, FR-4.2, NFR-SAFE-3 |
| System (end to end) | Every endpoint through real HTTP on a random port: FR-1 users, wallets, deposits; FR-2 transfers and idempotency; FR-3 concurrency; FR-4 reversals and immutability; FR-5 reconciliation incl. the tamper test; FR-6 queries; FR-0 cross-cutting behaviour | `src/test/java/.../api/*ApiTest` | TQ-3, FR-0-FR-6 |
| Negative | Negative-balance attempts, insufficient funds, missing/malformed/reused idempotency keys, malformed IDs, simultaneous transfers (FR-3.4: 20 x 100 from 1000; FR-3.5: 2 x 600 from 1000) | `TransferApiTest`, `IdempotencyApiTest`, `ConcurrencyApiTest`, `DepositApiTest`, `ReversalApiTest` | TQ-4 |
| Precision | Integer minor units end to end; 10.5, 1e3, "100" and true rejected; 1000 x 1 transfers exact; 2^53 - 1 exact; no floating-point type in code or schema | `PrecisionApiTest`, `AmountLimitApiTest`, `inspection/MoneyTypesInspectionTest` | TQ-6, CON-3, CI-2, NFR-REL-3, NFR-REL-6 |
| Security | 401/403 on every endpoint, validation of all parameters, negative-amount prevention, injection attempts, no internals in errors, keys never logged or returned | `SecurityApiTest`, `CrossCuttingApiTest`, `AccessLogTest`, `config/ApiKeysTest` | TQ-SEC, SEC-1-SEC-7 |
| Contract | `docs/api/openapi.yaml` lists exactly the operations, request fields, response fields and error codes of the code | `api/OpenApiContractTest`, `tests/test_api_docs.py` | SE-7, CON-4, FR-0.2 |
| Non-functional | Latency, throughput, start-up and reconciliation time; hard kill and restart under load | `scripts/nfr/load_test.py`, `scripts/nfr/crash_test.py` (run by hand, results in this folder) | NFR-PERF-1-5, NFR-SAFE-3, NFR-SAFE-4, NFR-REL-2 |
| Skills and tooling | Each skill's evaluation, the CI validators and scripts | `tests/test_*.py`, each `SKILL.md` "Evaluation" section, `skill-evaluations.md` | TQ-7, SK-0-SK-5 |
| Regression | Every significant defect gets a GitHub Issue (label `bug`) and a test that failed before the fix | listed in `defect-log.md` | TQ-8 |

## 2. Test data and isolation

- Every test run uses fresh database files in a new folder of the system temporary directory, deleted at the end of
  the run - never `data/ledger.db` (TQ-5, NFR-SAFE-5, AS-1). The system tests share one database per run; tests that
  must not touch it (fault injection, amount limit, tamper detection, docs switched off) get their own.
- API keys for tests are generated at random for each run; no key is written in the repository (CON-9, SEC-5).
- After every system test the base class runs reconciliation and fails the test unless it reports PASSED
  (NFR-REL-1); only the tamper test, which corrupts rows on purpose, restores them before it ends.
- Every concurrent test releases its requests at the same instant with a latch or barrier.

## 3. Tools (pinned, SEC-9)

JUnit 5, Spring Boot Test and Mockito (via `spring-boot-starter-test` of Spring Boot 3.5.8), the JDK HTTP client,
JaCoCo 0.8.15 (coverage), Checkstyle 10.23.1 (lint), Python 3 `unittest` (skills and scripts), gitleaks 8.30.1
(secrets). Postman collection `docs/api/financial-ledger.postman_collection.json` for manual testing.

## 4. Continuous integration

`.github/workflows/ci.yml` runs on **every push and every pull request** on GitHub-hosted Ubuntu runners (OE-4).
All four jobs are required checks of the `main` branch protection, so a failing job blocks the merge (PR-3, PR-5).

| Job | Steps |
|---|---|
| build, lint, test | JDK 21 with a cache of the Maven downloads; `mvn -B -ntp verify` = Checkstyle (fails on any violation), compile, every Java test on isolated databases, JaCoCo report and gate (at least 80 % line coverage in each of the `service`, `domain` and `persistence` packages, NFR-MAINT-2), package; then `scripts/ci/coverage_summary.py` prints the coverage table into the job summary; then the simulate-transfer skill runs end to end against the built jar (SK-5) |
| secret scan | gitleaks over the full git history (SEC-8) |
| validate SKILL.md files | `validate_skills.py` (SK-0, SB-3) and every Python test in `tests/` (TQ-8) |
| validate brain notes | `validate_brain.py` (SB-0, SB-1, DR-8), `validate_adrs.py` (SE-3, SB-3), `check_note.py` on inbox notes (SK-1) |

Before pushing, run locally (`AGENTS.md`): the four validators, `python3 -m unittest discover -s tests -v`,
`sh ./mvnw -B verify` and `python3 skills/local-check/scripts/local_check.py --by "<name>"`.

## 5. Entry and exit criteria

- **A pull request may merge** when all four CI jobs are green, a member other than the author has approved it, and it
  names its Issue and requirement IDs (PR-2, PR-3, AI-1).
- **Stage 3 is complete** when every FR, NFR and TQ requirement has at least one passing test or recorded measurement
  (`docs/traceability.md`), every skill evaluation is recorded (TQ-7), every defect in `defect-log.md` is closed with
  its regression test (TQ-8), and the Stage 3 review note exists (SB-2).

## 6. Defects (TQ-8)

A failing test or a wrong result is a defect: open a GitHub Issue labelled `bug` with the steps to reproduce, write a
test that fails, fix the code, and close the Issue from the pull request that adds the fix and the test. The pull
request also adds a row to `defect-log.md`.

## 7. Results (Stage 3)

Recorded at the Stage 3 wrap-up (#46) from `sh ./mvnw -B verify`, `python3 -m unittest discover -s tests` and the
scripts named below; the same numbers appear in every CI run of the final `main`.

### Java tests - 166 in 42 classes, all PASS

| Level | Test classes | Tests |
|---|---|---|
| Unit | `domain` (ErrorCodeTest, MoneyAndLedgerMathTest), `support` (Sha256, HashChain, RequestFingerprint, CanonicalJson, UuidsAndTimestamps), `config` (ApiKeys, SupportedCurrencies), `api.PagingTest`, `service.PostingServiceTest`, `inspection.MoneyTypesInspectionTest` | 39 |
| Integration | `persistence` (TransactionRunner, MigrationRunner, SchemaConstraints, SqlScript, Repositories, SqliteConnectionSettings), `service.SystemAccountsTest`, `testsupport.TestDatabaseTest`, `api.FaultInjectionTest`, `api.BusyApiTest` | 37 |
| System (HTTP) | Users, Wallets, Deposit, Transfer, Idempotency, Concurrency, Transactions, Reversal, Reconciliation, TamperDetection, AmountLimit, Precision, Security, CrossCutting, AccessLog, Health (2 classes), Docs (2 classes) | 84 |
| Contract | `api.OpenApiContractTest` | 6 |

Every system test ended with a reconciliation report of PASSED (NFR-REL-1). The FR-3.4 test (20 x 100 from 1000)
always gave exactly 10 x 201 and 10 x 422 with a final balance of 0; the FR-3.5 test (2 x 600 from 1000) exactly one
success and a final balance of 400.

### Line coverage (JaCoCo, NFR-MAINT-2) - gate met

| Package | Lines covered | Line coverage | Gate |
|---|---|---|---|
| `service` | 323/335 | 96.4 % | >= 80 % PASS |
| `domain` | 72/72 | 100.0 % | >= 80 % PASS |
| `persistence` | 234/254 | 92.1 % | >= 80 % PASS |
| `api` | 327/354 | 92.4 % | - |
| `config` | 80/80 | 100.0 % | - |
| `support` | 24/28 | 85.7 % | - |

### Python tests - 57, all PASS

`test_brain_tools` 8, `test_brain_refine` 7, `test_brain_query` 7, `test_local_check` 8, `test_validate_adrs` 5,
`test_coverage_summary` 4, `test_simulate_transfer` 8, `test_nfr_tools` 6, `test_api_docs` 4.

### Continuous integration

Every Stage 3 pull request (issues #37 to #46) was merged only with all four required jobs green; the job summary of
each "build, lint, test" run shows the coverage table, and its last step prints the simulate-transfer report
(5/5 steps, reconciliation PASSED). Secret scanning found nothing (SEC-8).

### Non-functional requirements

- Performance (NFR-PERF-1 to NFR-PERF-5): `performance-results.md`, from `python3 scripts/nfr/load_test.py`.
- Crash safety and recovery (NFR-SAFE-3, NFR-SAFE-4, NFR-REL-2): `crash-test-results.md`, from
  `python3 scripts/nfr/crash_test.py`.

### Skills and defects

- Skill evaluations of all five skills: `skill-evaluations.md`, Stage 3 section (TQ-7).
- Defects D1-D5, each with its Issue and regression test: `defect-log.md` (TQ-8).
