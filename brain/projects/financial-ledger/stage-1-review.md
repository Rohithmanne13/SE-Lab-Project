---
title: "Stage 1 review: requirements"
type: review
status: distilled
tags: [review, stage-1]
created: 2026-11-20
author: Manne Rohith Sai
related: [SB-2, SE-1, SB-0, SB-4, SK-1, PR-5, "#9", "#16", "PR #18"]
---

# Stage 1 review: requirements

Written under SB-2 at the end of Stage 1. Draft by Rohith; every member should add their own points in the review of this pull request.

## What we delivered

- SRS (FR, NFR, SK): `docs/srs/Team007_SRS_Submission.pdf` (15 pages), indexed in [[srs-requirements]].
- Brain as an Obsidian vault with PARA folders, [[index]] and [[log]] - rules in [[second-brain-workflow]].
- brain-ingest skill (format in [[agent-skills-format]]) used to add 13 research notes, with an evaluation recorded in `docs/testing/skill-evaluations.md`.
- AGENTS.md, README, CONTRIBUTING (branching strategy), CHANGELOG, development plan, traceability matrix and the CI skeleton.

## What worked

- Keeping the course's requirement IDs unchanged made it easy to trace every deliverable back to [[sdlc-and-workflow]] and [[project-statement-financial-ledger]].
- Automated checks (requirement-ID comparisons for the SRS; CI validators for notes and skills) caught mistakes that reading by eye missed - see [[ai-session-2026-11-11-srs-condensing]].
- Reading primary sources early surfaced Stage 2 design questions, e.g. in [[idempotency-key-header-draft]].
- Reviewing PR #18 was easy to verify: four local commands and the four CI checks confirmed the vault, the skill and the notes, and each note's Source link made the fidelity check simple. (Suhith)

## What did not work

- Our first SRS was 27 pages, but the teaching assistants set a limit of 10-15 pages, so we had to condense it after it was finished.
- The work was concentrated on one member's laptop and AI sessions; the other members' notes and reviews come after the main setup (SB-6).
- One planned source (the Medium second-brain article) could not be downloaded automatically - see [[ai-session-2026-11-20-stage-1-setup]].
- Most of Stage 1 was built before the rest of the team joined, so the review and my note came at the end of the stage, and Dhanush's and Abhiram's notes moved to Stage 2. (Suhith)

## What to change in Stage 2

- Check length and format limits with the teaching assistants before writing a document.
- Split work by GitHub Issue from the start of the stage so each member commits and reviews in parallel.
- Run the CI validators locally before opening a pull request.
- Record the language and framework decision (CON-6) as the first ADR, citing a distilled note (SB-3).
- Every member should start at the beginning of each stage and take their own GitHub Issues, so reviews and notes are not left for the end. (Suhith)
