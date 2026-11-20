# Financial Ledger & Digital Wallet API

Team 007 - Software Engineering Lab, IIIT Bangalore (Project 2).

A backend that exposes a RESTful API to create users and per-currency wallets, move money between wallets with idempotent transfers, and keep a tamper-evident double-entry ledger in SQLite, so that money is never lost, created or double-charged, even under retries and concurrent requests.

**Status: Stage 1 - Requirements.** The SRS is submitted; the second brain, the brain-ingest skill and the repository/CI skeleton are in place. The API itself is built in Stage 3.

## Team

| Member | Roll no. |
|---|---|
| Manne Rohith Sai | BT2024144 |
| Sunnapu Suhith Reddy | BT2024066 |
| Rudraraju Dhanush Varma | BT2024211 |
| Abhiram P B S | BT2024166 |

## Repository layout

| Path | What it is |
|---|---|
| `docs/srs/` | Submitted SRS - the requirements baseline (PDF and DOCX). |
| `docs/course/` | Course material: project statement, SDLC document, IEEE SRS template. |
| `docs/development-plan.md` | Development plan and task breakdown per stage (SE-4). |
| `docs/traceability.md` | Traceability matrix: requirement -> issue -> PR -> test -> skill -> brain note (SE-5). |
| `docs/testing/` | Test documentation, including skill evaluation results (TQ-7). |
| `brain/` | Obsidian second-brain vault, organised with PARA; start at `brain/index.md`. |
| `skills/` | Agent Skills (`SKILL.md` folders); currently `brain-ingest`. |
| `scripts/ci/` | Validators used by CI. |
| `tests/` | Automated tests. |
| `AGENTS.md` | Rules for AI coding agents working in this repository. |
| `CONTRIBUTING.md` | Workflow, branching strategy, reviews, releases. |
| `CHANGELOG.md` | Changes per release. |

## Prerequisites

- Git and a GitHub account with access to this repository.
- Python 3.10 or later (validators and skill scripts use only the standard library).
- [Obsidian](https://obsidian.md) for the `brain/` vault (no account needed).
- The language runtime and build tool for the service are chosen in a Stage 2 ADR (SRS CON-6); this section is updated then.

## Getting started

```bash
git clone https://github.com/Rohithmanne13/SE-Lab-Project.git
cd SE-Lab-Project
python3 scripts/ci/validate_brain.py
python3 scripts/ci/validate_skills.py
python3 -m unittest discover -s tests -v
```

### Open the second brain in Obsidian

1. Open Obsidian and choose **Open folder as vault**.
2. Select the `brain` folder inside your clone.
3. Start at `index.md`. Rules for notes and links: `resources/second-brain-workflow.md`.

### Add a research note

Use the brain-ingest skill: ask your AI coding agent to "run the brain-ingest skill on <URL or file>", or follow `skills/brain-ingest/SKILL.md` by hand. Then open a pull request.

## Continuous integration

`.github/workflows/ci.yml` runs on every push and pull request:

| Job | Checks |
|---|---|
| build, lint, test | Builds, lints and tests the service once the build tool exists (Stage 2 ADR); skips with a notice until then. |
| secret scan | gitleaks over the full git history (SEC-8). |
| validate SKILL.md files | Agent Skills format, required sections, distilled-note citation (SK-0, SB-3); regression tests (TQ-8). |
| validate brain notes | PARA structure, DR-8 front matter, broken links (SB-0, SB-1, DR-8); brain-ingest output rules (SK-1). |

## How we work

See `CONTRIBUTING.md`: Issues for all work, one branch per issue, pull requests with at least one approving review, CI must pass, Semantic Versioning tags and `CHANGELOG.md`.
