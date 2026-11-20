---
title: "AI session 2026-11-11: condensing the SRS to 15 pages"
type: ai-session
status: distilled
tags: [ai-session, srs, stage-1]
created: 2026-11-20
author: Manne Rohith Sai
related: [SE-1, AI-1, AI-2, AI-3, "#1"]
---

# AI session 2026-11-11: condensing the SRS to 15 pages

Recorded under AI-3. Tool: Claude Code (model Claude Opus 5.5), driving python-docx and Microsoft Word on Rohith's laptop. Session run by Manne Rohith Sai; follow-up checks continued until 2026-11-20.

## Goal

The teaching assistants limited the SRS to about 10-15 pages. Reduce our 27-page SRS to at most 15 pages while keeping every requirement of [[project-statement-financial-ledger]], [[sdlc-and-workflow]] and the section structure of [[ieee-srs-template]]. Result: `docs/srs/Team007_SRS_Submission.pdf` / `.docx`, indexed in [[srs-requirements]].

## What worked

- Rebuilding the document from a script and rendering it with Word made layout iterations repeatable (18 -> 16 -> 15 pages).
- Automated checks instead of reading by eye: comparing requirement IDs, error codes, references and key numbers between the old and new versions, and checking every heading against the template text.
- Those checks caught real losses that a visual review missed: DE-2, DE-3 and DE-4 had been merged into one row, an error code was abbreviated, and "three-letter" had been dropped from FR-1.3. All were restored.
- Checking against the actual template file showed that each feature needed the template's numbered sub-sections (x.1, x.2, x.3) and that the Table of Contents comes before the Revision History.

## What failed or needed correction

- The first condensed build was 18 pages; typography alone was not enough, so structural changes (merged priority tables, compact stimulus/response) were needed.
- Some assistant statements had to be corrected later in the session (for example, a summary that still described the revision history as "1.0" after it had been changed to "Stage 1 SRS"). Lesson: re-verify claims against the files, not against earlier messages.
- Human decisions overrode defaults: removing "Version 1.0 approved" from the cover, using "Stage 1 SRS" in the revision history, and matching the template's front-matter order exactly.

## Human review

Rohith reviewed every change and approved the final files (AI-1, AI-2). The SRS was submitted for Stage 1 and is frozen.
