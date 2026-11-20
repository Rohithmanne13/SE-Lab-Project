# Traceability matrix

Required by SRS **SE-5**: requirement -> issue -> PR -> test -> skill -> brain note. Seeded from SRS Appendix D; the Issue and PR columns are filled as work is opened and merged.
Requirement statements: [`brain/projects/financial-ledger/srs-requirements.md`](../brain/projects/financial-ledger/srs-requirements.md). Verification methods: T = test, D = demonstration, I = inspection, A = analysis.

## Stage 1 deliverables

| Requirement | Issue | PR | Test / verification | Skill | Brain note |
|---|---|---|---|---|---|
| SE-1 SRS with FR, NFR, SK | - | - | I: `docs/srs/Team007_SRS_Submission.pdf` | - | `srs-requirements`, `ai-session-2026-11-11-srs-condensing` |
| SB-0 vault: PARA, index, log | | | T: `validate_brain.py` (CI job "validate brain notes") | brain-ingest | `second-brain-workflow` |
| SB-1 links to requirement IDs, issues, PRs | | | T: `validate_brain.py` broken-link check | brain-ingest | `second-brain-workflow`, `obsidian-internal-links` |
| SB-2 stage review note | | | I: `stage-1-review` | - | `stage-1-review` |
| SB-3 skills cite a distilled note | | | T: `validate_skills.py` | brain-ingest | `agent-skills-format`, `second-brain-workflow` |
| SB-4 AGENTS.md | | | I: `AGENTS.md` | - | `sdlc-and-workflow` |
| SB-5 brain changes through PRs | | | I: PR history, branch protection | - | `second-brain-workflow` |
| SB-6 every member authors notes | | | I: commit history, note `author` fields | brain-ingest | `second-brain-workflow` |
| SK-0 skill format | | | T: `validate_skills.py`; S1-S4 in `docs/testing/skill-evaluations.md` | brain-ingest | `agent-skills-format`, `agent-skills-specification` |
| SK-1 brain-ingest, at least 10 notes | | | T: `check_note.py` E1-E7, N1-N6; `tests/test_brain_tools.py` | brain-ingest | 13 notes in `brain/inbox/` |
| DR-8 note front matter | | | T: `validate_brain.py` schema check | brain-ingest | `second-brain-workflow` |
| PR-1 commits, branching strategy | | | I: `CONTRIBUTING.md`, commit history | - | `sdlc-and-workflow` |
| PR-2 GitHub Issues | | | I: Issues list | - | `sdlc-and-workflow` |
| PR-3 reviewed PRs, branch protection | | | I: PR reviews, branch protection settings | - | `sdlc-and-workflow` |
| PR-4 SemVer tags, CHANGELOG | | | I: `CHANGELOG.md`, tag `v0.1.0` | - | `keep-a-changelog`, `semantic-versioning` |
| PR-5 CI on every PR | | | I: `.github/workflows/ci.yml`, CI runs | - | `sdlc-and-workflow` |
| SEC-8 secret scanning | | | T: CI job "secret scan" | - | - |
| SEC-9 pinned dependencies | | | I: action SHAs and gitleaks version/checksum in `ci.yml` | - | - |
| SE-4 development plan | | | I: `docs/development-plan.md` | - | - |
| SE-5 traceability matrix | | | I: this file | - | `srs-requirements` |
| TQ-7 skill evaluations | | | I: `docs/testing/skill-evaluations.md` | brain-ingest | - |
| TQ-8 regression tests for defects | | | T: `tests/test_brain_tools.py` | - | - |
| AI-3 ai-session notes | | | I: `ai-session-*` notes | - | `ai-session-2026-11-20-stage-1-setup` |

## Product and quality requirements (seeded from SRS Appendix D)

| Requirement | Method | Issue | PR | Test / verification | Skill | Brain note |
|---|---|---|---|---|---|---|
| FR-0.1 - FR-0.5 | T | | | Unit + system tests; negative tests for 401/403/400/413/415 | local-check | `rfc-9457-problem-details` |
| FR-1.1 - FR-1.9 | T, D | | | Unit (ledger balance); system E2E for FR-1 (TQ-3); negative tests (TQ-4) | simulate-transfer (1-2) | `project-statement-financial-ledger` |
| FR-2.1 - FR-2.10 | T, D | | | Unit (fingerprint hashing, TQ-1); integration (atomic key record, TQ-2); E2E (TQ-3); negative (TQ-4) | simulate-transfer (3) | `idempotency-key-header-draft` |
| FR-3.1 - FR-3.6 | T, D | | | Concurrent system tests FR-3.4/FR-3.5 (TQ-4); integration test of lock timeout | simulate-transfer (4) | `sqlite-transactions`, `sqlite-file-locking` |
| FR-4.1 - FR-4.7 | T, I | | | Integration: UPDATE/DELETE blocked by triggers; hash-chain unit tests; reversal E2E (TQ-3) | - | `project-statement-financial-ledger` |
| FR-5.1 - FR-5.7 | T, D | | | System E2E; tamper test (modify a row with triggers disabled -> FAILED report) | simulate-transfer (5) | `sqlite-write-ahead-logging` |
| FR-6.1 - FR-6.5 | T | | | System tests; pagination tests | local-check (4) | - |
| NFR-PERF-1 - 5 | T, A | | | Load-test script results recorded in test documentation | - | - |
| NFR-SAFE-1 - 5 | T | | | Reconciliation after every test; fault-injection tests (TQ-2) | simulate-transfer | `sqlite-file-locking` |
| SEC-1 - SEC-9 | T, I | | | Security tests (TQ-SEC); CI secret scanning; code review; dependency-version inspection | - | - |
| NFR-REL / MAINT / PORT / USE / OBS | T, I, D | | | TQ-6 precision tests; CI coverage report; local-check on all OSs; README walkthrough; log inspection | local-check | `sqlite-write-ahead-logging` |
| OE, CON, AS, UI, SI, CI | I, T | | | Configuration and code inspection; interfaces covered by FR system tests | local-check | `sqlite-transactions` |
| SK-0 - SK-5 | D, T | | | Skill evaluations (TQ-7); CI SKILL.md validation | each skill | `agent-skills-format` |
| TQ-1 - TQ-8, TQ-SEC | I | | | CI run history, test reports, defect log | - | `project-statement-financial-ledger` |
| SB-0 - SB-6, DR-8 | I | | | Vault structure, CI note validation, commit history, review notes | brain-ingest / refine / query | `second-brain-workflow` |
| SE-1 - 7, PR-1 - 5, AI-1 - 4 | I | | | Repository inspection: docs, ADRs, issues, PRs, reviews, tags, ai-session notes | - | `sdlc-and-workflow` |
| DR-1 - DR-7, BR-1 - BR-9 | T, I | | | Schema inspection; constraint tests; reconciliation | simulate-transfer | - |
