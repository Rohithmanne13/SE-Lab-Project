---
title: "Stage 2 review: design"
type: review
status: distilled
tags: [review, stage-2]
created: 2026-11-27
author: Rudraraju Dhanush Varma
related: [SB-2, SE-2, SE-3, SK-2, SK-3, SK-4, "#27"]
---

# Stage 2 review: design

Written under SB-2 at the end of Stage 2. Each member adds their own points, signed with their name.

## What we delivered

- Architecture, data model and behaviour design with UML/ER diagrams in `docs/design/` (SE-2) and ADRs 0001-0004 in `docs/adr/` (SE-3), each citing distilled notes such as [[sqlite-transactions]] (SB-3).
- The brain-refine skill, which filed every inbox note into PARA - see [[second-brain-workflow]] - and the brain-query skill with a 15-question evaluation set (SK-2, SK-3).
- A Spring Boot service skeleton with `GET /health` and the CI build/lint/test job switched on, and the first version of the local-check skill (SK-4).

## What worked

- Running brain-refine with a script made every move identical, and its tests caught a bug that would have mislabelled archived notes in the index before it ever ran on the real vault. (Dhanush)
- The evaluation set for brain-query gave a concrete target: all 15 questions find their expected notes, so we know the search works before relying on it. (Dhanush)

## What did not work

- Every brain pull request edits `index.md` and `log.md`, so brain pull requests have to be opened one after another from the latest `main` to avoid merge conflicts. (Dhanush)

## What to change in Stage 3

- Run local-check before every push, not only before a stage submission, now that the service builds. (Dhanush)
