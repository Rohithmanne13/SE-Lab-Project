# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.2.0] - 2026-11-27

Stage 2 - Design.

### Added

- Architecture, data model and behaviour design with UML/ER diagrams and an error-code catalogue in `docs/design/` (SE-2).
- Architecture Decision Records ADR-0001 to ADR-0004 in `docs/adr/`, with a CI check that each cites distilled notes (SE-3, SB-3, CON-6).
- brain-refine skill; every inbox note filed into PARA folders and marked distilled (SK-2).
- brain-query skill with a 15-question evaluation set (SK-3).
- local-check skill: build, test, start, `GET /health`, report (SK-4).
- Spring Boot service skeleton (Java 21, Maven) with `GET /health`, SQLite connection settings, Checkstyle lint and tests; the CI build/lint/test job now builds (FR-6.4, SI-2, PR-5).
- Research notes on Superpowers and OpenAPI 3.1.0; Stage 2 review note; skill evaluation results (SB-6, SB-2, TQ-7).

### Fixed

- brain-ingest removes the empty-inbox placeholder written by brain-refine.

## [0.1.0] - 2026-11-23

Stage 1 - Requirements.

### Added

- Software Requirements Specification (FR, NFR, SK) in `docs/srs/` (SE-1); course material in `docs/course/`.
- Obsidian second-brain vault `brain/` with PARA folders, `index.md`, `log.md` and an SRS requirement index (SB-0, SB-1).
- brain-ingest Agent Skill with helper and evaluation scripts (SK-0, SK-1); 13 research notes added with it.
- Distilled notes on the vault workflow and the skill format (SB-3); Stage 1 review note (SB-2); AI-session notes (AI-3).
- `AGENTS.md` (SB-4), `README.md`, `CONTRIBUTING.md` with the branching strategy (PR-1) and this changelog (PR-4).
- CI skeleton on GitHub Actions: build/lint/test, secret scanning, SKILL.md and brain-note validation (PR-5, SEC-8, SEC-9).
- Development plan (SE-4), traceability matrix (SE-5), skill evaluation record (TQ-7) and regression tests (TQ-8).

[Unreleased]: https://github.com/Rohithmanne13/SE-Lab-Project/compare/v0.2.0...HEAD
[0.2.0]: https://github.com/Rohithmanne13/SE-Lab-Project/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/Rohithmanne13/SE-Lab-Project/releases/tag/v0.1.0
