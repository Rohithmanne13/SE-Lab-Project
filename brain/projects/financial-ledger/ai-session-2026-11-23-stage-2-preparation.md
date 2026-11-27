---
title: "AI session 2026-11-23: preparing the Stage 2 deliverables"
type: ai-session
status: distilled
tags: [ai-session, stage-2, design, skills]
created: 2026-11-23
author: Manne Rohith Sai
related: [SE-2, SE-3, SK-2, SK-3, SK-4, CON-6, AI-1, AI-2, AI-3, "#21", "#22", "#23", "#24", "#25", "#26", "#27"]
---

# AI session 2026-11-23: preparing the Stage 2 deliverables

Recorded under AI-3. Tool: Claude Code (model Claude Opus 5.5). Session run by Manne Rohith Sai; the work was then committed and pushed in pull requests by Rudraraju Dhanush Varma and Abhiram P B S, and reviewed by another member (AI-1).

## Goal

Prepare every Stage 2 deliverable of [[sdlc-and-workflow]] strictly within [[srs-requirements]]: design documentation with UML/ER diagrams, ADRs, the brain-refine and brain-query skills, the first local-check skill, and the notes deferred from Stage 1 - packaged as step-by-step pull requests for Dhanush and Abhiram.

## What worked

- Rehearsing every pull request in order on a scratch copy of the repository, with all checks run after each one, before writing the hand-off guides.
- Choosing the stack from evidence on the team's machine (Spring Boot already used) and recording it in ADR-0001 rather than deciding silently.
- Executing the designed SQLite schema to prove each trigger and constraint, and parsing all 13 Mermaid diagrams with Mermaid itself.
- Writing tests for each new script before the real run: they caught the index-relabelling bug in brain-refine before it touched the vault.

## What failed or needed correction

- brain-refine first rewrote links inside `index.md` and could not tell who ran it; both fixed before the first real run.
- The empty-inbox placeholder written by brain-refine was not removed by brain-ingest; fixed with a regression test.
- Two Mermaid sequence diagrams were invalid because `;` ends a statement in Mermaid; found only by an automated parse.

## Human review

Each pull request is reviewed and approved by a member other than its author before merge (AI-1, PR-3); every member must be able to explain the merged work (AI-2).
