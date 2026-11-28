# Traceability matrix

Required by SRS **SE-5**: requirement -> issue -> PR -> test -> skill -> brain note. Seeded from SRS Appendix D; the Issue and PR columns are filled as work is opened and merged.
Requirement statements: [`brain/projects/financial-ledger/srs-requirements.md`](../brain/projects/financial-ledger/srs-requirements.md). Verification methods: T = test, D = demonstration, I = inspection, A = analysis.

## Stage 1 deliverables

| Requirement | Issue | PR | Test / verification | Skill | Brain note |
|---|---|---|---|---|---|
| SE-1 SRS with FR, NFR, SK | #1 | PR #18 | I: `docs/srs/Team007_SRS_Submission.pdf` | - | `srs-requirements`, `ai-session-2026-11-11-srs-condensing` |
| SB-0 vault: PARA, index, log | #2 | PR #18 | T: `validate_brain.py` (CI job "validate brain notes") | brain-ingest | `second-brain-workflow` |
| SB-1 links to requirement IDs, issues, PRs | #2 | PR #18 | T: `validate_brain.py` broken-link check | brain-ingest | `second-brain-workflow`, `obsidian-internal-links` |
| SB-2 stage review note | #9, #16 | PR #18 | I: `stage-1-review` | - | `stage-1-review` |
| SB-3 skills cite a distilled note | #2, #3 | PR #18 | T: `validate_skills.py` | brain-ingest | `agent-skills-format`, `second-brain-workflow` |
| SB-4 AGENTS.md | #5 | PR #18 | I: `AGENTS.md` | - | `sdlc-and-workflow` |
| SB-5 brain changes through PRs | #12 | PR #18 | I: PR history, branch protection | - | `second-brain-workflow` |
| SB-6 every member authors notes | #4, #13, #14, #15 | PR #18 (+ member PRs) | I: commit history, note `author` fields | brain-ingest | `second-brain-workflow` |
| SK-0 skill format | #3 | PR #18 | T: `validate_skills.py`; S1-S4 in `docs/testing/skill-evaluations.md` | brain-ingest | `agent-skills-format`, `agent-skills-specification` |
| SK-1 brain-ingest, at least 10 notes | #3, #4 | PR #18 | T: `check_note.py` E1-E7, N1-N6; `tests/test_brain_tools.py` | brain-ingest | 13 notes in `brain/inbox/` |
| DR-8 note front matter | #2 | PR #18 | T: `validate_brain.py` schema check | brain-ingest | `second-brain-workflow` |
| PR-1 commits, branching strategy | #6 | PR #18 | I: `CONTRIBUTING.md`, commit history | - | `sdlc-and-workflow` |
| PR-2 GitHub Issues | #1-#17 | - | I: Issues list | - | `sdlc-and-workflow` |
| PR-3 reviewed PRs, branch protection | #12, #16 | PR #18 | I: PR reviews, branch protection settings | - | `sdlc-and-workflow` |
| PR-4 SemVer tags, CHANGELOG | #6, #17 | PR #18 | I: `CHANGELOG.md`, tag `v0.1.0` | - | `keep-a-changelog`, `semantic-versioning` |
| PR-5 CI on every PR | #7 | PR #18 | I: `.github/workflows/ci.yml`, CI runs | - | `sdlc-and-workflow` |
| SEC-8 secret scanning | #7 | PR #18 | T: CI job "secret scan" | - | - |
| SEC-9 pinned dependencies | #7 | PR #18 | I: action SHAs and gitleaks version/checksum in `ci.yml` | - | - |
| SE-4 development plan | #8 | PR #18 | I: `docs/development-plan.md` | - | - |
| SE-5 traceability matrix | #8 | PR #18 | I: this file | - | `srs-requirements` |
| TQ-7 skill evaluations | #9 | PR #18 | I: `docs/testing/skill-evaluations.md` | brain-ingest | - |
| TQ-8 regression tests for defects | #10, #11, #19 | PR #18 | T: `tests/test_brain_tools.py` | - | - |
| AI-3 ai-session notes | #9 | PR #18 | I: `ai-session-*` notes | - | `ai-session-2026-11-20-stage-1-setup` |

## Stage 2 deliverables

PR numbers are linked from each closed issue.

| Requirement | Issue | Test / verification | Skill | Brain note / document |
|---|---|---|---|---|
| SE-2 architecture and design, UML/ER diagrams | #24 | I: `docs/design/` (13 Mermaid diagrams, all parse) | - | `docs/design/architecture.md`, `data-model.md`, `behaviour.md` |
| SE-3 ADRs citing brain notes | #23 | T: `validate_adrs.py` (CI), `tests/test_validate_adrs.py` | brain-query (drafting) | ADR-0001 to ADR-0004 |
| CON-6 implementation language | #23 | I: ADR-0001; T: CI build | - | `sqlite-transactions`, `openapi-specification-3-1` |
| CON-5, SE-6 layers and provider interfaces | #24, #25 | T: `HealthControllerDegradedTest` (substituted `DatabaseHealth`) | - | `docs/design/architecture.md` |
| SB-3 ADRs cite distilled notes | #23 | T: `validate_adrs.py` | - | ADR Sources sections |
| SK-2 brain-refine | #21 | T: `refine.py check`, `tests/test_brain_refine.py` | brain-refine | `second-brain-workflow`, `para-method` |
| SK-3 brain-query | #22 | T: `eval_query.py` (15/15), `tests/test_brain_query.py` | brain-query | `second-brain-workflow` |
| SK-4 local-check | #26 | T: local-check run 5/5, `tests/test_local_check.py` | local-check | `agent-skills-format` |
| FR-6.4 GET /health | #25 | T: `HealthEndpointTest`, `HealthControllerDegradedTest` | local-check (4) | `docs/design/behaviour.md` 8 |
| SI-2 connection settings | #25 | T: `SqliteConnectionSettingsTest` | - | ADR-0002 |
| OE-3, DR-6 configuration | #25 | I: `application.properties`; T: tests use `ledger.db-path` | local-check | `docs/design/architecture.md` 6 |
| PR-5, TQ-5 CI build/lint/test on isolated DB | #25 | T: CI job "build, lint, test" (`mvn -B -ntp verify`) | - | ADR-0001 |
| SB-6 every member authors notes | #14, #15 | I: commit history, note `author` fields | brain-ingest | `superpowers-skill-set`, `openapi-specification-3-1` |
| SB-2 stage review note | #27 | I: `stage-2-review` | - | `stage-2-review` |
| TQ-7 skill evaluations | #27 | I: `docs/testing/skill-evaluations.md` Stage 2 | all skills | - |
| TQ-8 regression tests | #27 | T: refine, ingest regression tests | - | - |
| AI-3 ai-session notes | #27 | I: `ai-session-2026-11-23-stage-2-preparation` | - | `ai-session-2026-11-23-stage-2-preparation` |
| PR-4 release | #28 | I: `CHANGELOG.md` 0.2.0, tag `v0.2.0` | - | `keep-a-changelog`, `semantic-versioning` |

## Stage 3 deliverables

PR numbers are linked from each closed issue. Test names are JUnit classes in `src/test/java/edu/iiitb/team007/ledger/`
unless a path is given.

| Requirement | Issue | Test / verification | Skill | Brain note / document |
|---|---|---|---|---|
| SB-2 stage review note | #41, #46 | I: `stage-3-review` | - | `stage-3-review` |
| TQ-7 skill evaluations | #46 | I: `docs/testing/skill-evaluations.md` Stage 3 | all five skills | - |
| TQ-8 defect log with regression tests | #48-#52 | I: `docs/testing/defect-log.md`; T: the regression test named in each row | - | `stage-3-review` |
| PR-5, TQ-5 continuous testing and CI plan/results | #38, #46 | I: `docs/testing/test-plan.md`; T: CI jobs on every push and pull request | - | - |
| NFR-MAINT-2 coverage reported by CI | #38 | T: JaCoCo gate in `mvn verify`; `scripts/ci/coverage_summary.py`, `tests/test_coverage_summary.py` | - | - |
| SK-5 simulate-transfer with results | #43 | T: `simulate_transfer.py` run (CI and `--by`), `tests/test_simulate_transfer.py` | simulate-transfer | `agent-skills-format`, `sqlite-transactions` |
| SE-7, UI-1 OpenAPI and Swagger UI | #42 | T: `api/OpenApiContractTest`, `api/DocsApiTest`, `api/DocsDisabledTest` | - | `openapi-specification-3-1` |
| UI-2 client collection | #45 | T: `tests/test_api_docs.py`; D: every request run against the service | - | - |
| NFR-USE-1 README, user guide | #45 | I: `README.md`, `docs/user-guide.md` | local-check | - |
| AI-3 ai-session note | #41 | I: `ai-session-2026-11-28-stage-3-build` | - | `ai-session-2026-11-28-stage-3-build` |
| PR-4 release | #47 | I: `CHANGELOG.md` 0.3.0, tag `v0.3.0` | - | `keep-a-changelog`, `semantic-versioning` |

## Product and quality requirements (seeded from SRS Appendix D)

| Requirement | Method | Issue | Test / verification | Skill | Brain note |
|---|---|---|---|---|---|
| FR-0.1 - FR-0.5 | T | #39, #42 | `api/CrossCuttingApiTest`, `api/SecurityApiTest`, `api/AccessLogTest`, `config/ApiKeysTest`; negative tests for 401/403/400/413/415 | local-check | `rfc-9457-problem-details` |
| FR-1.1 - FR-1.9 | T, D | #37, #39, #40 | `api/UsersApiTest`, `api/WalletsApiTest`, `api/DepositApiTest`, `service/SystemAccountsTest`, `domain/MoneyAndLedgerMathTest`, `service/PostingServiceTest` | simulate-transfer (1-2) | `project-statement-financial-ledger` |
| FR-2.1 - FR-2.10 | T, D | #40 | `support/RequestFingerprintTest`, `support/CanonicalJsonTest` (TQ-1); `api/FaultInjectionTest` (TQ-2); `api/TransferApiTest`, `api/IdempotencyApiTest` (TQ-3, TQ-4) | simulate-transfer (3) | `idempotency-key-header-draft` |
| FR-3.1 - FR-3.6 | T, D | #37, #40 | `api/ConcurrencyApiTest` (FR-3.4, FR-3.5), `api/BusyApiTest`, `persistence/TransactionRunnerTest` (lock timeout) | simulate-transfer (4) | `sqlite-transactions`, `sqlite-file-locking` |
| FR-4.1 - FR-4.7 | T, I | #37, #41 | `persistence/SchemaConstraintsTest` (triggers), `support/HashChainTest`, `api/ReversalApiTest`, `api/TransactionsApiTest` (no PUT/PATCH/DELETE) | - | `project-statement-financial-ledger` |
| FR-5.1 - FR-5.7 | T, D | #41 | `api/ReconciliationApiTest`, `api/TamperDetectionApiTest` (rows changed with triggers dropped -> FAILED) | simulate-transfer (5) | `sqlite-write-ahead-logging` |
| FR-6.1 - FR-6.5 | T | #25, #39, #40 | `api/WalletsApiTest`, `api/TransactionsApiTest` (pagination), `api/HealthEndpointTest`, `api/HealthControllerDegradedTest`, `persistence/TransactionRunnerTest` (reader snapshot) | local-check (4) | - |
| NFR-PERF-1 - 5 | T, A | #44 | `scripts/nfr/load_test.py`, results `docs/testing/performance-results.md`; `api/ConcurrencyApiTest` (NFR-PERF-3) | - | - |
| NFR-SAFE-1 - 5 | T | #40, #41, #44 | reconciliation after every system test; `api/FaultInjectionTest` (TQ-2); `scripts/nfr/crash_test.py`, results `docs/testing/crash-test-results.md`; `persistence/SqliteConnectionSettingsTest` (synchronous FULL) | simulate-transfer | `sqlite-file-locking` |
| SEC-1 - SEC-9 | T, I | #39, #41 | `api/SecurityApiTest` (TQ-SEC), `api/CrossCuttingApiTest`, `api/AccessLogTest`, `config/ApiKeysTest`; CI secret scanning; dependency versions in `pom.xml` | - | - |
| NFR-REL / MAINT / PORT / USE / OBS | T, I, D | #38, #40, #41, #44, #45 | `api/PrecisionApiTest`, `api/AmountLimitApiTest`, `inspection/MoneyTypesInspectionTest` (TQ-6); `api/ReconciliationApiTest` (NFR-REL-5); JaCoCo gate (NFR-MAINT-2); local-check on every member's machine (NFR-PORT-1); README (NFR-USE-1); `api/AccessLogTest` (NFR-OBS-1) | local-check | `sqlite-write-ahead-logging` |
| OE, CON, AS, UI, SI, CI | I, T | #37, #39, #42 | `persistence/SqliteConnectionSettingsTest` (SI-2), `persistence/MigrationRunnerTest` (SI-4), `api/OpenApiContractTest` (CON-4), `api/DocsApiTest` (UI-1), `api/CrossCuttingApiTest` (CI-3, CI-4) | local-check | `sqlite-transactions` |
| SK-0 - SK-5 | D, T | #43, #46 | Skill evaluations (TQ-7); CI SKILL.md validation | each skill | `agent-skills-format` |
| TQ-1 - TQ-8, TQ-SEC | I | #38, #46 | `docs/testing/test-plan.md` (levels and results), CI run history, `docs/testing/defect-log.md` | - | `project-statement-financial-ledger` |
| SB-0 - SB-6, DR-8 | I | #41, #46 | Vault structure, CI note validation, commit history, review notes | brain-ingest / refine / query | `second-brain-workflow` |
| SE-1 - 7, PR-1 - 5, AI-1 - 4 | I | #37-#47 | Repository inspection: docs, ADRs, issues, PRs, reviews, tags, ai-session notes | - | `sdlc-and-workflow` |
| DR-1 - DR-7, BR-1 - BR-9 | T, I | #37, #40, #41 | `persistence/SchemaConstraintsTest`, `persistence/MigrationRunnerTest`, `service/PostingServiceTest`; reconciliation | simulate-transfer | - |
