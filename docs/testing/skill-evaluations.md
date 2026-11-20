# Skill evaluations

Test documentation for SRS **TQ-7**: skill evaluations run before each stage submission. Each skill's `## Evaluation` section defines its checks; results are recorded here, newest first.

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
