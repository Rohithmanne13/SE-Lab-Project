# Financial Ledger & Digital Wallet API

Team 007 - Software Engineering Lab, IIIT Bangalore (Project 2).

A backend that exposes a RESTful API to create users and per-currency wallets, move money between wallets with idempotent transfers, and keep a tamper-evident double-entry ledger in SQLite, so that money is never lost, created or double-charged, even under retries and concurrent requests.

**Status: Stage 2 - Design.** Architecture and design docs with UML/ER diagrams, ADRs, the brain-refine, brain-query and local-check skills, and a service skeleton with `GET /health` are in place. The full API is built in Stage 3.

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
| `docs/design/` | Architecture, data model, behaviour (UML/ER diagrams) and error codes (SE-2). |
| `docs/adr/` | Architecture Decision Records (SE-3). |
| `src/` | Java service (Spring Boot): `main/` code, `test/` tests (ADR-0001). |
| `pom.xml`, `mvnw`, `.mvn/`, `config/checkstyle/` | Maven build (via the Maven Wrapper) and lint rules. |
| `docs/course/` | Course material: project statement, SDLC document, IEEE SRS template. |
| `docs/development-plan.md` | Development plan and task breakdown per stage (SE-4). |
| `docs/traceability.md` | Traceability matrix: requirement -> issue -> PR -> test -> skill -> brain note (SE-5). |
| `docs/testing/` | Test documentation, including skill evaluation results (TQ-7). |
| `brain/` | Obsidian second-brain vault, organised with PARA; start at `brain/index.md`. |
| `skills/` | Agent Skills (`SKILL.md` folders): `brain-ingest`, `brain-refine`, `brain-query`, `local-check`. |
| `scripts/ci/` | Validators used by CI. |
| `tests/` | Automated tests. |
| `AGENTS.md` | Rules for AI coding agents working in this repository. |
| `CONTRIBUTING.md` | Workflow, branching strategy, reviews, releases. |
| `CHANGELOG.md` | Changes per release. |

## Prerequisites

- Git and a GitHub account with access to this repository.
- Python 3.10 or later (validators and skill scripts use only the standard library).
- [Obsidian](https://obsidian.md) for the `brain/` vault (no account needed).
- JDK 21 for the service (ADR-0001). On Ubuntu/WSL: `sudo apt install -y openjdk-21-jdk`; check with `java -version`. Maven is not needed: the Maven Wrapper (`sh ./mvnw`, on Windows `mvnw.cmd`) downloads the pinned Maven 3.9.11 on first use.

## Getting started

```bash
git clone https://github.com/Rohithmanne13/SE-Lab-Project.git
cd SE-Lab-Project
python3 scripts/ci/validate_brain.py
python3 scripts/ci/validate_skills.py
python3 -m unittest discover -s tests -v
sh ./mvnw -B verify                   # lint, compile, test and package the service
python3 skills/local-check/scripts/local_check.py --by "<your full name>"   # SK-4: build, test, start, GET /health
```

### Run the service

```bash
sh ./mvnw -B -DskipTests package
java -jar target/financial-ledger-api.jar          # http://127.0.0.1:8000
curl http://127.0.0.1:8000/health                   # {"status":"ok","database":"ok","version":"..."}
```

Settings come from environment variables: `LEDGER_HOST` (default `127.0.0.1`), `LEDGER_PORT` (`8000`), `LEDGER_DB_PATH` (`data/ledger.db`) - see `docs/design/architecture.md`.

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
| build, lint, test | `mvn -B -ntp verify` on JDK 21: Checkstyle, compile, tests on an isolated database, package (PR-5, TQ-5). |
| secret scan | gitleaks over the full git history (SEC-8). |
| validate SKILL.md files | Agent Skills format, required sections, distilled-note citation (SK-0, SB-3); regression tests (TQ-8). |
| validate brain notes | PARA structure, DR-8 front matter, broken links (SB-0, SB-1, DR-8); ADR sections and citations (SE-3, SB-3); brain-ingest output rules (SK-1). |

## How we work

See `CONTRIBUTING.md`: Issues for all work, one branch per issue, pull requests with at least one approving review, CI must pass, Semantic Versioning tags and `CHANGELOG.md`.
