# Skill evaluations

Test documentation for SRS **TQ-7**: skill evaluations run before each stage submission. Each skill's `## Evaluation` section defines its checks; results are recorded here, newest first.

## Stage 3 - all skills

- **Date:** Stage 3 submission (#46)
- **Overall:** PASS

### simulate-transfer (SK-5)

| Run | Result |
|---|---|
| Sunnapu Suhith Reddy, own machine, 2026-11-28 (`--by`) | PASS 5/5 steps, reconciliation PASSED |
| CI, job "build, lint, test", every push and pull request | PASS 5/5 steps, reconciliation PASSED (job summary of each run) |
| `python3 -m unittest tests.test_simulate_transfer` | PASS - 8 tests: a correct API passes; a retried transfer posted twice fails step 3; two concurrent transfers both let through fail step 4; a FAILED reconciliation fails step 5; a setup failure stops the run; unknown member, missing keys and missing jar are rejected |

Balances of every run (minor units): source 100000 and destination 0 at the start, 75000 and 25000 after the
idempotent transfer and its replay, 25000 and 75000 at the end. SK-5 acceptance (all steps PASS and reconciliation
PASSED): **met**.

### local-check (SK-4)

| Member | OS | Result |
|---|---|---|
| Manne Rohith Sai | Linux 6.18.58.1-microsoft-standard-WSL2 | PASS 5/5 steps - build, tests, server, `GET /health` 200, report (2026-11-28) |
| Sunnapu Suhith Reddy | Linux 6.18.58.1-microsoft-standard-WSL2 | PASS 5/5 steps - build, tests, server, `GET /health` 200, report (2026-11-28) |
| Rudraraju Dhanush Varma | Linux 6.9.3-76060903-generic | PASS 5/5 steps (Stage 2 run, below) |
| Abhiram P B S | Linux 6.18.58.1-microsoft-standard-WSL2 | PASS 5/5 steps (Stage 2 run, below) |

Since Stage 3 the service needs API keys; local-check starts it with throwaway random keys (defect D2, #49).
`python3 -m unittest tests.test_local_check` - PASS, 8 tests. SK-4 acceptance (passes on every member's machine):
**met** for all four members (NFR-PORT-1).

### brain-ingest (SK-1), brain-refine (SK-2), brain-query (SK-3)

| Check | Result |
|---|---|
| brain-ingest: `python3 -m unittest tests.test_brain_tools` | PASS - 8 tests; no research notes were added in Stage 3 and the inbox is empty |
| brain-refine: `refine.py check` | PASS - inbox empty, 0 broken links (SK-2 acceptance) |
| brain-query: `eval_query.py` - 15 fixed questions | PASS - 15/15 with the two new Stage 3 notes in the vault (an expected note in the top 3, all in the top 5) |
| brain-query: logged query | "What happens when BEGIN IMMEDIATE cannot get the write lock?" - cited `srs-requirements`, `sqlite-transactions` (answer: SQLITE_BUSY after the busy timeout, mapped to 503 SERVICE_BUSY, FR-3.6) |
| `python3 -m unittest tests.test_brain_refine tests.test_brain_query` | PASS - 14 tests |

### Supporting checks

| Check | Result |
|---|---|
| `validate_skills.py` | PASS - 5 skills |
| `validate_adrs.py` | PASS - 4 ADRs |
| `validate_brain.py` | PASS - 28 notes, 0 errors |
| `sh ./mvnw -B verify` | PASS - 0 Checkstyle violations, 166 tests, coverage gate met (service 96.4 %, domain 100 %, persistence 92.1 %) |
| `python3 -m unittest discover -s tests` | PASS - 57 tests |

### Defects found during Stage 3

Five, D1-D5 (issues #48-#52), each closed with a regression test - see `defect-log.md`.

## Stage 2 - all skills

- **Date:** Stage 2 submission (each member adds their local-check run to the table below)
- **Overall:** PASS

### brain-ingest (SK-1)

| Check | Result |
|---|---|
| Notes added in Stage 2 with the skill | 2: `superpowers-skill-set` (Dhanush, #14), `openapi-specification-3-1` (Abhiram, #15) - both passed E1-E7 when created |
| `check_note.py` after brain-refine | PASS - inbox empty, all notes refined into PARA folders |
| `python3 -m unittest tests.test_brain_tools` | PASS - 8 tests |

### brain-refine (SK-2)

| Check | Result |
|---|---|
| First refine run | 16 inbox notes moved: 3 to `projects/financial-ledger`, 4 to `areas/` (second-brain, release-management), 9 to `resources/` (sqlite, http-api, ai-tooling); all `status: distilled` |
| `refine.py check` (SK-2 acceptance) | PASS - inbox empty, 0 broken links |
| `validate_brain.py` | PASS - 25 notes, 214 links, 0 errors |
| `python3 -m unittest tests.test_brain_refine` | PASS - 7 tests (move, archive, keep, merge, relink, 12 refusal cases writing nothing) |

### brain-query (SK-3)

| Check | Result |
|---|---|
| `eval_query.py` - 15 fixed questions | PASS - 15/15 found their expected notes (an expected note in the top 3, all in the top 5) |
| `python3 -m unittest tests.test_brain_query` | PASS - 7 tests |
| Logged query | "Which HTTP status code should be returned when an Idempotency-Key is reused with a different payload?" - cited `srs-requirements`, `idempotency-key-header-draft` (answer: 422, FR-2.7) |
| Reviewer check (3 evaluation questions run and their notes read) | Done by the reviewer of the brain-query pull request (#22) - see the review comment there |

### local-check (SK-4)

| Member | OS | Result |
|---|---|---|
| Rudraraju Dhanush Varma | Linux 6.9.3-76060903-generic | PASS 5/5 steps - build, tests, server, `GET /health` 200, report |
| Abhiram P B S | Linux 6.18.58.1-microsoft-standard-WSL2 | PASS 5/5 steps - build, tests, server, `GET /health` 200, report |
| Sunnapu Suhith Reddy | _add your run_ | |
| Manne Rohith Sai | _add your run_ | |

`python3 -m unittest tests.test_local_check` - PASS, 7 tests.

### Supporting checks

| Check | Result |
|---|---|
| `validate_skills.py` | PASS - 4 skills |
| `validate_adrs.py` | PASS - 4 ADRs, each citing distilled notes (SB-3); `tests.test_validate_adrs` PASS - 5 tests |
| `mvn -B verify` | PASS - 0 Checkstyle violations, 5 tests (FR-6.4, SI-2, SE-6) |

### Defects found during Stage 2

1. `refine.py` rewrote links inside `index.md`, which would have relabelled an archived duplicate's index entry - fixed before first use; covered by `test_merge_keeps_index_entry_of_the_archived_duplicate`.
2. brain-ingest kept the new empty-inbox placeholder `_(none yet)_` beside a new entry - fixed; covered in `test_created_note_passes_vault_validation`.
3. Two Mermaid sequence diagrams used `;` inside messages, which Mermaid treats as a statement end - fixed; all 13 diagrams parse.

## Stage 1 - brain-ingest (SK-1)

- **Date:** 2026-11-20
- **Run by:** Manne Rohith Sai (with Claude Code), branch `feature/stage-1-setup`
- **Commands:** `python3 skills/brain-ingest/scripts/check_note.py`, `python3 scripts/ci/validate_brain.py`, `python3 scripts/ci/validate_skills.py`
- **Overall:** PASS (human fidelity review pending - see check 4)

### 1. Output checks E1-E7 on every inbox note

| Note | E1 inbox | E2 DR-8 | E3 sections | E4 links | E5 source | E6 index | E7 log | Result |
|---|---|---|---|---|---|---|---|---|
| project-statement-financial-ledger | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ | PASS |
| sdlc-and-workflow | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ | PASS |
| ieee-srs-template | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ | PASS |
| sqlite-transactions | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ | PASS |
| sqlite-write-ahead-logging | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ | PASS |
| sqlite-file-locking | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ | PASS |
| rfc-9457-problem-details | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ | PASS |
| idempotency-key-header-draft | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ | PASS |
| agent-skills-specification | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ | PASS |
| para-method | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ | PASS |
| obsidian-internal-links | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ | PASS |
| keep-a-changelog | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ | PASS |
| semantic-versioning | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ | PASS |

Result: 13/13 notes PASS. SK-1 acceptance (at least 10 research notes in Stage 1): **met**.

### 2. Vault and skill validators

| Check | Result |
|---|---|
| `validate_brain.py` - PARA folders, index/log, DR-8 schema, broken links | PASS - 21 notes, 172 links, 0 errors |
| `validate_skills.py` - SKILL.md naming, sections, distilled-note citation | PASS - 1 skill, 0 errors |

### 3. Negative cases (each must be rejected and change nothing)

| # | Case | Expected | Result |
|---|---|---|---|
| N1 | Ingest with an existing slug | Rejected, no overwrite | PASS |
| N2 | Unknown requirement ID (`FR-9.9`) | Rejected | PASS |
| N3 | Body without `## Summary` | Rejected | PASS |
| N4 | Author outside Team 007 | Rejected | PASS |
| N5 | Body links to a note that does not exist | Rejected | PASS |
| N6 | No requirement ID in `--related` | Rejected | PASS |
| - | `index.md`, `log.md` and `inbox/` after N1-N6 | Unchanged | PASS (checksums equal) |
| V1-V7 | Validator given: bad author, missing `created`, broken note link, broken requirement anchor, distilled note in inbox, invalid `type`, missing `log.md` | Each reported as an error, exit 1 | PASS (7/7) |
| S1-S4 | Skill validator given: invalid name, missing `## Failure handling`, no distilled note cited, unknown front-matter field | Each reported as an error, exit 1 | PASS (4/4) |

### 4. Fidelity review (human)

A reviewer compares at least two notes with their sources and confirms that every summary sentence and key point appears in the source (SK-1: "never invents content absent from the source").

| Note | Reviewer | Result |
|---|---|---|
| sqlite-transactions | AI cross-check (Claude Code), 2026-11-21 | PASS - 15/15 key-point claims matched to verbatim passages in sqlite.org/lang_transaction.html; only labelled "Team observation" lines are not from the source |
| rfc-9457-problem-details | AI cross-check (Claude Code), 2026-11-21 | PASS - 18/18 key-point claims matched to verbatim passages in RFC 9457; only labelled "Team observation" lines are not from the source |
| _(at least 2 notes)_ | _human reviewer - pending (PR #18 review, issue #16)_ | |

The AI cross-check is supporting evidence only: the AI that wrote the notes cannot be their independent reviewer (AI-1), so a team member still performs the human check.

### Defects found during the evaluation

1. `ingest.py` treated link examples inside inline code as real links and rejected a valid note - fixed; code is now ignored, as in the CI validator (#10).
2. The shared link parser read an escaped `\|` inside a table link as part of the heading name - fixed (#11).
3. Issue/PR references written unquoted in `related:` are YAML comments, so Obsidian dropped them - fixed; references are now quoted (#19).
