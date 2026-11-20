---
title: "The AI-Assisted SDLC and workflow (course document)"
type: research
status: inbox
tags: [course, process, second-brain]
created: 2026-11-20
author: Manne Rohith Sai
related: [SE-1, SE-2, SE-3, SE-4, SE-5, SE-6, SE-7, SB-0, SB-1, SB-2, SB-3, SB-4, SB-5, SB-6, PR-1, PR-2, PR-3, PR-4, PR-5, AI-1, AI-2, AI-3, AI-4, TQ-7, TQ-8, SK-1, SK-2, SK-3, SK-4, "#4", "PR #18"]
---

# The AI-Assisted SDLC and workflow (course document)

## Summary

The course document that defines the engineering practices, CI/CD requirements and AI-assisted workflow common to both course projects: the second brain, software-engineering requirements, the common `local-check` skill, GitHub workflow, testing, the AI policy and the deliverables of the four stages.

## Key points

- Second brain: PARA structure (`brain/inbox/`, `projects/`, `areas/`, `resources/`, `archive/`); `brain/index.md` is the entry point; `brain/log.md` records all runs. Maintained with three skills: brain-ingest, brain-refine, brain-query.
- SB-1 wikilinks between notes and to requirement IDs, issues and PRs; SB-2 a review note at the end of each stage; SB-3 every ADR and skill cites at least one distilled note; SB-4 an AGENTS.md at the repository root; SB-5 brain changes go through pull requests; SB-6 every member authors notes, visible in commit history.
- SE-1 to SE-7: SRS with FR, NFR and SK; architecture/design docs with UML/ER; ADRs in `docs/adr/` citing brain notes; development plan and task breakdown; traceability matrix (requirement -> issue -> PR -> test -> skill -> brain note); external dependencies behind a provider interface; user and API documentation (e.g. OpenAPI).
- `local-check` skill: build, run tests, start the local server, call one read-only fetch endpoint, report pass/fail per step.
- GitHub: meaningful incremental commits from every member, documented branching strategy, Issues for all work and defects, PRs with at least one review for every change, release tags and a changelog. CI on every PR: build, lint, tests, secret scanning, SKILL.md validation, brain-note validation.
- TQ-7 skill evaluations before each stage submission (results in test documentation); TQ-8 maintain tests and add a regression test per significant defect.
- AI policy: the team is responsible for every merged line and a human reviews every AI-generated change; every member can explain merged work; significant AI sessions are recorded as `ai-session` notes; no secrets in AI tools.
- Stage 1 deliverables: SRS (FR, NFR, SK); brain as an Obsidian vault (PARA, index, log); brain-ingest used to add at least 10 research notes; AGENTS.md; repository and CI skeleton.

## Links

- The product-specific half of the course brief is [[project-statement-financial-ledger]]; both are refined in [[srs-requirements]] (sections 6.1-6.8).

## Requirements

- [[srs-requirements#SE-1|SE-1]]
- [[srs-requirements#SE-2|SE-2]]
- [[srs-requirements#SE-3|SE-3]]
- [[srs-requirements#SE-4|SE-4]]
- [[srs-requirements#SE-5|SE-5]]
- [[srs-requirements#SE-6|SE-6]]
- [[srs-requirements#SE-7|SE-7]]
- [[srs-requirements#SB-0|SB-0]]
- [[srs-requirements#SB-1|SB-1]]
- [[srs-requirements#SB-2|SB-2]]
- [[srs-requirements#SB-3|SB-3]]
- [[srs-requirements#SB-4|SB-4]]
- [[srs-requirements#SB-5|SB-5]]
- [[srs-requirements#SB-6|SB-6]]
- [[srs-requirements#PR-1|PR-1]]
- [[srs-requirements#PR-2|PR-2]]
- [[srs-requirements#PR-3|PR-3]]
- [[srs-requirements#PR-4|PR-4]]
- [[srs-requirements#PR-5|PR-5]]
- [[srs-requirements#AI-1|AI-1]]
- [[srs-requirements#AI-2|AI-2]]
- [[srs-requirements#AI-3|AI-3]]
- [[srs-requirements#AI-4|AI-4]]
- [[srs-requirements#TQ-7|TQ-7]]
- [[srs-requirements#TQ-8|TQ-8]]
- [[srs-requirements#SK-1|SK-1]]
- [[srs-requirements#SK-2|SK-2]]
- [[srs-requirements#SK-3|SK-3]]
- [[srs-requirements#SK-4|SK-4]]

Issues / PRs: #4, PR #18

## Source

- docs/course/SDLC_and_Workflow.pdf
- Ingested 2026-11-20 by Manne Rohith Sai with the brain-ingest skill.
