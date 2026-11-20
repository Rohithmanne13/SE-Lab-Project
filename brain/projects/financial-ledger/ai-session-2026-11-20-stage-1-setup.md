---
title: "AI session 2026-11-20: setting up the Stage 1 deliverables"
type: ai-session
status: distilled
tags: [ai-session, stage-1, second-brain, ci]
created: 2026-11-20
author: Manne Rohith Sai
related: [SB-0, SB-4, SK-0, SK-1, PR-1, PR-5, TQ-7, AI-1, AI-3]
---

# AI session 2026-11-20: setting up the Stage 1 deliverables

Recorded under AI-3. Tool: Claude Code (model Claude Opus 5.5). Session run by Manne Rohith Sai on branch `feature/stage-1-setup`.

## Goal

Build every remaining Stage 1 deliverable from [[sdlc-and-workflow]], strictly within [[srs-requirements]] and nothing extra: the `brain/` vault (PARA, index, log), the brain-ingest skill and at least 10 research notes, AGENTS.md, and the repository and CI skeleton.

## What worked

- Reading the sources before writing notes: 13 research notes were ingested with the brain-ingest skill from the course documents and primary sources (SQLite docs, RFC 9457, the Idempotency-Key draft, the Agent Skills specification, PARA, Obsidian, Keep a Changelog, SemVer).
- Putting the mechanical parts of ingestion in a script (front matter, index, log, no overwrite) made every run identical, and the script rejects a note rather than writing a half-finished one.
- Generating [[srs-requirements]] straight from the submitted SRS gave all 179 requirement IDs as link targets without retyping them.
- Reading the sources surfaced design questions for Stage 2, recorded in [[idempotency-key-header-draft]] (409 for in-flight duplicates, quoted header values).

## What failed or needed correction

- The Medium article "How I Built My Second Brain with Obsidian + Claude Code" blocks automated download (HTTP 403), so it was not ingested; a team member can add it from a browser copy.
- The Obsidian help page renders with JavaScript; its official Markdown source on GitHub was used instead.
- Bug found by the skill itself: the ingest script treated link examples inside `code` as real links and rejected the Obsidian note. Fixed so the script and the CI validator both ignore code.
- Bug found while writing the distilled notes: an escaped `\|` inside a table link was read as part of the heading name. Fixed in the shared link parser.
- The laptop shut down mid-session; nothing had been written yet, so no work was lost.

## Human review

All of this arrives as one pull request; a team member other than the author must review and approve it before merge (AI-1, PR-3).
