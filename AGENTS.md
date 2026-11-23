# AGENTS.md

Instructions for AI coding agents (Claude Code, Copilot, Codex and similar) working in this repository. Required by SRS SB-4. Humans: see `README.md` and `CONTRIBUTING.md`.

## Project

**Financial Ledger & Digital Wallet API** - Team 007, Software Engineering Lab, IIIT Bangalore. A REST backend for per-currency wallets, idempotent transfers and a tamper-evident double-entry ledger on SQLite.

## Source of truth: the submitted SRS

- `docs/srs/Team007_SRS_Submission.pdf` (and `.docx`) is the frozen requirements baseline. Every requirement ID and statement is mirrored in `brain/projects/financial-ledger/srs-requirements.md`.
- **Implement everything the SRS specifies and nothing it does not.** Before changing anything, find the requirement ID(s) the change satisfies and cite them in the commit message and pull request. If a request has no basis in the SRS, stop and ask the team instead of adding it.
- Decisions the SRS leaves open (e.g. the implementation language, CON-6) are recorded as ADRs in `docs/adr/`, never as new requirements, and never change the SRS.
- Course material: `docs/course/` (project statement, SDLC document, IEEE SRS template).

## Repository map

| Path | Contents |
|---|---|
| `brain/` | Obsidian second-brain vault (PARA). Entry point `brain/index.md`; every skill run is logged in `brain/log.md`. |
| `skills/<skill-name>/SKILL.md` | Agent Skills (SK-0). Current: `brain-ingest`. Planned: `brain-refine`, `brain-query`, `local-check` (Stage 2), `simulate-transfer` (Stage 3). |
| `scripts/ci/` | CI validators for the vault and the skills. |
| `tests/` | Automated tests (regression tests for tooling now; product tests from Stage 3). |
| `docs/` | SRS, course material, development plan, traceability matrix, test documentation; ADRs in `docs/adr/` from Stage 2. |
| `.github/workflows/ci.yml` | CI: build/lint/test, secret scan, SKILL.md validation, brain-note validation. |

## Commands

Run from the repository root (Python 3, standard library only):

```bash
python3 scripts/ci/validate_brain.py          # vault: PARA, index/log, DR-8 front matter, broken links
python3 scripts/ci/validate_skills.py         # every SKILL.md: name, sections, distilled-note citation
python3 -m unittest discover -s tests -v      # regression tests
python3 skills/brain-ingest/scripts/check_note.py   # brain-ingest evaluation on inbox notes
```

Run all four before opening a pull request; CI runs the same checks.

## Working with the brain (`brain/`)

- Follow `brain/resources/second-brain-workflow.md`.
- Every note starts with DR-8 front matter: `title`, `type` (research | concept | decision | ai-session | review | meeting), `status` (inbox | distilled | archived), `tags`, `created` (YYYY-MM-DD), `author` (a Team 007 member), `related` (requirement IDs, `#issue`, `PR #n`).
- Link notes with `[[note-name]]` and requirements with `[[srs-requirements#FR-2.4|FR-2.4]]`. No broken links.
- Add research only with the brain-ingest skill. Write summaries and key points **only from the source**; never invent content.
- Never edit `srs-requirements.md` by hand; the SRS is frozen.
- Record significant AI sessions as `type: ai-session` notes in `brain/projects/financial-ledger/` (goal, what worked, what failed) (AI-3).

## Working with skills (`skills/`)

- Follow `brain/resources/agent-skills-format.md`: front matter `name` (= folder name) and `description`; sections Inputs, Steps, Outputs, Failure handling, Evaluation; cite at least one distilled brain note (SB-3).
- When you run a skill, follow its SKILL.md exactly and report each step as PASS or FAIL (UI-4).
- Record evaluation results in `docs/testing/skill-evaluations.md` before each stage submission (TQ-7).

## Git and review rules

- Never commit to `main`. Create a branch named as in `CONTRIBUTING.md`, commit in small meaningful steps, open a pull request that references its GitHub Issue.
- Every AI-generated change is reviewed and approved by a team member other than its author before merge (AI-1, PR-3). The team must be able to explain every merged line (AI-2).
- Every significant defect gets a GitHub Issue and a regression test (TQ-8).
- Update `CHANGELOG.md` under `## [Unreleased]` for user-visible changes (PR-4).

## Security

- Never put secrets, tokens, API keys or passwords in the repository, in notes, in logs or in prompts to AI tools (CON-9, SEC-5, AI-4). Keys come from environment variables; only a dummy `.env.example` may be committed.
- Pin third-party dependencies and actions to exact versions (SEC-9).
- CI secret scanning must pass (SEC-8).
