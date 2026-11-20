# Development plan and task breakdown

Required by SRS **SE-4**; kept up to date as work proceeds. Scope is exactly the submitted SRS (`docs/srs/Team007_SRS_Submission.pdf`) - nothing outside it is planned. Deliverables per stage follow SRS section 6.8.

Every task becomes a GitHub Issue (PR-2) and is delivered through a reviewed pull request (PR-3). Owners are recorded in the Issue; the traceability matrix (`docs/traceability.md`, SE-5) links each requirement to its issue, PR, test, skill and brain note.

## Team

| Member | Roll no. | GitHub |
|---|---|---|
| Manne Rohith Sai | BT2024144 | Rohithmanne13 |
| Sunnapu Suhith Reddy | BT2024066 | _(add)_ |
| Rudraraju Dhanush Varma | BT2024211 | _(add)_ |
| Abhiram P B S | BT2024166 | _(add)_ |

## Stage 1 - Requirements

| # | Task | Requirements | Owner | Status |
|---|---|---|---|---|
| 1.1 | SRS with FR, NFR and SK requirements | SE-1 | Team | Done (submitted) |
| 1.2 | Obsidian vault `brain/` with PARA folders, `index.md`, `log.md` | SB-0 | Rohith | Done |
| 1.3 | brain-ingest skill (SKILL.md, helper and evaluation scripts) | SK-0, SK-1 | Rohith | Done |
| 1.4 | At least 10 research notes added with brain-ingest | SK-1 | Rohith (13 notes) | Done |
| 1.5 | One research note each, ingested with brain-ingest from the member's own account | SB-6, SK-1 | Suhith: Model Context Protocol intro; Dhanush: Superpowers skill set; Abhiram: OpenAPI 3.1.0 | To do |
| 1.6 | AGENTS.md at the repository root | SB-4 | Rohith | Done |
| 1.7 | Repository skeleton: README, CONTRIBUTING (branching strategy), CHANGELOG | PR-1, PR-4, SE-7 | Rohith | Done |
| 1.8 | CI skeleton: build/lint/test, secret scan, SKILL.md and brain-note validation | PR-5, SEC-8, SEC-9 | Rohith | Done |
| 1.9 | Development plan (this file) and traceability matrix | SE-4, SE-5 | Rohith | Done |
| 1.10 | Skill evaluation run, results recorded | TQ-7 | Rohith; fidelity review by a reviewer | Done; fidelity review in PR |
| 1.11 | Stage 1 review note and AI-session notes | SB-2, AI-3 | Rohith; all members add points | Done (draft) |
| 1.12 | GitHub Issues for the Stage 1 work; branch protection on `main` (1 approving review, CI required) | PR-2, PR-3 | Rohith | To do |
| 1.13 | Review and merge the Stage 1 PR; tag release `v0.1.0` and update CHANGELOG | PR-3, PR-4 | Reviewer: another member; tag: Rohith | To do |

## Stage 2 - Design

| # | Task | Requirements |
|---|---|---|
| 2.1 | ADR-0001: implementation language, framework and build tool (citing a distilled note) | CON-6, SE-3, SB-3 |
| 2.2 | Architecture and design documentation with UML and ER diagrams | SE-2, CON-5, DR-1 |
| 2.3 | Further ADRs for significant decisions (e.g. idempotency storage, locking, hash-chain serialisation) | SE-3, FR-2.4, FR-3.1, FR-4.4 |
| 2.4 | brain-refine skill | SK-2 |
| 2.5 | brain-query skill | SK-3 |
| 2.6 | First version of local-check skill | SK-4 |
| 2.7 | Turn on the build/lint/test CI job for the chosen build tool | PR-5, TQ-5 |
| 2.8 | Skill evaluations; Stage 2 review note | TQ-7, SB-2 |

## Stage 3 - Testing and CI

| # | Task | Requirements |
|---|---|---|
| 3.1 | Implement FR-0 to FR-6, data requirements and business rules | FR-0-FR-6, DR-1-DR-7, BR-1-BR-9 |
| 3.2 | Unit, integration, system, negative, precision and security tests | TQ-1-TQ-6, TQ-SEC |
| 3.3 | Non-functional checks (performance, safety, security, quality attributes) | NFR-PERF, NFR-SAFE, SEC, NFR-REL, NFR-MAINT, NFR-PORT, NFR-USE, NFR-OBS |
| 3.4 | simulate-transfer skill with test results | SK-5 |
| 3.5 | Continuous testing and CI plan/results; defect log with regression tests | PR-5, TQ-8 |
| 3.6 | User and API documentation (OpenAPI, user guide, client collection) | SE-7, UI-1, UI-2 |
| 3.7 | Skill evaluations; Stage 3 review note | TQ-7, SB-2 |

## Stage 4 - Review

| # | Task | Requirements |
|---|---|---|
| 4.1 | Live demonstration of FR-1 to FR-5 in a client | FR-1-FR-5 |
| 4.2 | Run simulate-transfer end-to-end, local-check live and brain-query live | SK-5, SK-4, SK-3 |
| 4.3 | Walk through one requirement from brain note to ADR, code and test | SE-5 |
| 4.4 | Final release tag `v1.0.0` (SRS release 1.0) and CHANGELOG | PR-4 |
| 4.5 | Skill evaluations; Stage 4 review note | TQ-7, SB-2 |
