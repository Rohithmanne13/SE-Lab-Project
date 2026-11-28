# Financial Ledger & Digital Wallet API

Team 007 - Software Engineering Lab, IIIT Bangalore (Project 2).

A backend that exposes a RESTful API to create users and per-currency wallets, move money between wallets with idempotent transfers, and keep a tamper-evident double-entry ledger in SQLite, so that money is never lost, created or double-charged, even under retries and concurrent requests.

**Status: Stage 3 - Testing and CI.** The complete API of the SRS is implemented (FR-0 to FR-6) with unit, integration, system, negative, precision, security and concurrency tests, a coverage gate in CI, the simulate-transfer skill, the OpenAPI contract with Swagger UI, a Postman collection and the usage guide.

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
| `docs/api/` | API contract `openapi.yaml` (OpenAPI 3.1) and the Postman collection (SE-7, UI-2). |
| `docs/user-guide.md` | Step-by-step usage of FR-1 to FR-5 with cURL. |
| `docs/design/` | Architecture, data model, behaviour (UML/ER diagrams) and error codes (SE-2). |
| `docs/adr/` | Architecture Decision Records (SE-3). |
| `docs/testing/` | Test plan and results, defect log, skill evaluations, performance results (TQ-7, TQ-8). |
| `src/` | Java service (Spring Boot): `main/` code, `test/` tests (ADR-0001). |
| `pom.xml`, `mvnw`, `.mvn/`, `config/checkstyle/` | Maven build (via the Maven Wrapper) and lint rules. |
| `docs/course/` | Course material: project statement, SDLC document, IEEE SRS template. |
| `docs/development-plan.md` | Development plan and task breakdown per stage (SE-4). |
| `docs/traceability.md` | Traceability matrix: requirement -> issue -> PR -> test -> skill -> brain note (SE-5). |
| `brain/` | Obsidian second-brain vault, organised with PARA; start at `brain/index.md`. |
| `skills/` | Agent Skills (`SKILL.md` folders): `brain-ingest`, `brain-refine`, `brain-query`, `local-check`, `simulate-transfer`. |
| `scripts/ci/` | Validators and reports used by CI. |
| `scripts/nfr/` | Load, crash-recovery and timing checks of the non-functional requirements. |
| `tests/` | Python tests of the skills, scripts and documentation. |
| `.env.example` | Every configuration variable with a dummy value. |
| `AGENTS.md` | Rules for AI coding agents working in this repository. |
| `CONTRIBUTING.md` | Workflow, branching strategy, reviews, releases. |
| `CHANGELOG.md` | Changes per release. |

## Prerequisites

- Git and a GitHub account with access to this repository.
- JDK 21 (ADR-0001). On Ubuntu/WSL: `sudo apt install -y openjdk-21-jdk`; check with `java -version`. Maven is not needed: the Maven Wrapper (`sh ./mvnw`, on Windows `mvnw.cmd`) downloads the pinned Maven 3.9.11 on first use.
- Python 3.10 or later (validators, skills and NFR scripts use only the standard library).
- [Obsidian](https://obsidian.md) for the `brain/` vault (no account needed).

## Getting started (about 10 minutes, NFR-USE-1)

```bash
git clone https://github.com/Rohithmanne13/SE-Lab-Project.git
cd SE-Lab-Project
sh ./mvnw -B verify                   # lint, compile, all tests on a throwaway database, coverage gate, package
python3 -m unittest discover -s tests -v
python3 skills/local-check/scripts/local_check.py --by "<your full name>"   # SK-4: build, test, start, GET /health
```

### Run the service

The service needs two API keys from the environment (FR-0.1, SEC-5) - it refuses to start without them:

```bash
export LEDGER_CLIENT_KEYS="$(python3 -c 'import secrets; print(secrets.token_urlsafe(32))')"
export LEDGER_ADMIN_KEYS="$(python3 -c 'import secrets; print(secrets.token_urlsafe(32))')"
sh ./mvnw -B -q -DskipTests package
java -jar target/financial-ledger-api.jar          # http://127.0.0.1:8000
```

Then, in another terminal:

```bash
curl http://127.0.0.1:8000/health                   # {"status":"ok","database":"ok","version":"..."}
```

Open <http://127.0.0.1:8000/docs> for Swagger UI (UI-1) - click **Authorize** and paste a key to try requests. Walk through every feature with `docs/user-guide.md`, or import `docs/api/financial-ledger.postman_collection.json` into Postman and set its `clientKey` and `adminKey` variables.

### Configuration

All settings are environment variables (OE-3, DR-6); `.env.example` lists them with dummy values. Copy it to `.env` (git ignores it), fill in the keys and load it with `set -a; . ./.env; set +a`.

| Variable | Meaning | Default |
|---|---|---|
| `LEDGER_HOST` | bind address | `127.0.0.1` |
| `LEDGER_PORT` | HTTP port | `8000` |
| `LEDGER_DB_PATH` | SQLite database file | `data/ledger.db` |
| `LEDGER_CLIENT_KEYS` | comma-separated CLIENT API keys, each at least 32 random characters | none - required |
| `LEDGER_ADMIN_KEYS` | comma-separated ADMIN API keys | none - required |
| `LEDGER_CURRENCIES` | supported ISO 4217 codes; must include INR and USD | `INR,USD` |
| `LEDGER_DOCS_ENABLED` | serve `/docs` and `/openapi.yaml` (development mode) | `true` |

Never commit keys or paste them into an AI tool (CON-9, AI-4). Any deployment beyond localhost must sit behind TLS 1.2+ (CI-1).

### Run the skills

```bash
python3 skills/simulate-transfer/scripts/simulate_transfer.py --by "<your full name>"   # SK-5: full workflow + reconciliation
python3 skills/local-check/scripts/local_check.py --by "<your full name>"               # SK-4
python3 skills/brain-query/scripts/query.py search "idempotency key reused"             # SK-3
```

Each skill's `SKILL.md` describes its inputs, steps, outputs and failure handling; ask your AI coding agent to "run the <name> skill".

### Open the second brain in Obsidian

1. Open Obsidian and choose **Open folder as vault**.
2. Select the `brain` folder inside your clone.
3. Start at `index.md`. Rules for notes and links: `resources/second-brain-workflow.md`.

### Add a research note

Use the brain-ingest skill: ask your AI coding agent to "run the brain-ingest skill on <URL or file>", or follow `skills/brain-ingest/SKILL.md` by hand. Then open a pull request.

## Continuous integration

`.github/workflows/ci.yml` runs on every push and pull request; each job is a required check on `main` (PR-5). Details and results: `docs/testing/test-plan.md`.

| Job | Checks |
|---|---|
| build, lint, test | `mvn -B -ntp verify` on JDK 21: Checkstyle, compile, every test on an isolated database, JaCoCo coverage gate (at least 80 % of the service, domain and persistence lines, NFR-MAINT-2), package; coverage table in the job summary; then simulate-transfer end to end against the built jar (SK-5). |
| secret scan | gitleaks over the full git history (SEC-8). |
| validate SKILL.md files | Agent Skills format, required sections, distilled-note citation (SK-0, SB-3); Python tests (TQ-8). |
| validate brain notes | PARA structure, DR-8 front matter, broken links (SB-0, SB-1, DR-8); ADR sections and citations (SE-3, SB-3); brain-ingest output rules (SK-1). |

## How we work

See `CONTRIBUTING.md`: Issues for all work, one branch per issue, pull requests with at least one approving review, CI must pass, Semantic Versioning tags and `CHANGELOG.md`.
