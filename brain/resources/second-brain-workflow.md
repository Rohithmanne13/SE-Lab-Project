---
title: "Second brain workflow (how Team 007 runs the vault)"
type: concept
status: distilled
tags: [second-brain, para, obsidian, process]
created: 2026-11-20
author: Manne Rohith Sai
related: [SB-0, SB-1, SB-2, SB-3, SB-5, SB-6, DR-8, SK-1, SK-2, SK-3, AI-3, "#2", "PR #18"]
---

# Second brain workflow

Distilled from [[sdlc-and-workflow]], [[para-method]] and [[obsidian-internal-links]], applied to the requirements in [[srs-requirements]]. This is the rulebook the brain skills and AGENTS.md follow.

## Structure (SB-0)

- `brain/` is opened in Obsidian as a vault ("Open folder as vault"); no Obsidian account is needed.
- PARA, organised by actionability ([[para-method]]): `inbox/` (new, unsorted notes), `projects/` (active, deadline-driven work - ours is `projects/financial-ledger/`), `areas/` (ongoing responsibilities), `resources/` (distilled reference material), `archive/` (inactive notes).
- [[index]] is the entry point and lists every note; [[log]] gets one line for every skill run.

## Note format (DR-8)

Every note except `index.md` and `log.md` starts with front matter:

```yaml
---
title: "Human-readable title"
type: research        # research | concept | decision | ai-session | review | meeting
status: inbox         # inbox | distilled | archived
tags: [lowercase, words]
created: 2026-11-20   # YYYY-MM-DD
author: Manne Rohith Sai   # a Team 007 member (SB-6)
related: [FR-2, SB-1, "#12", "PR #15"]   # requirement IDs, issues, PRs (SB-1); quote # references
---
```

- Status follows the folder: `inbox` only in `inbox/`; `distilled` once a note is refined into projects/areas/resources; `archived` in `archive/`.
- File names are lowercase words joined by hyphens; names are unique across the vault, so a `[[name]]` link is never ambiguous.

## Links (SB-1)

- Link notes with wikilinks: `[[note-name]]`, display text with `[[note-name|text]]` ([[obsidian-internal-links]]).
- Link a requirement as a heading in the requirement index: `[[srs-requirements#FR-2.4|FR-2.4]]`.
- Reference GitHub issues and PRs in the `related` field (quoted, e.g. `"#12"`, because an unquoted `#` starts a YAML comment) and in the body as `#12` and `PR #15`.
- CI (`scripts/ci/validate_brain.py`) fails a pull request on any missing front-matter field or broken link.

## Lifecycle and skills

1. **Ingest** (Stage 1, skill `brain-ingest`): one source -> one research note in `inbox/`, summary and key points only from the source, plus index and log updates.
2. **Refine** (Stage 2, skill `brain-refine`): move inbox notes into PARA folders, merge duplicates, repair links, mark refined notes `status: distilled`.
3. **Query** (Stage 2, skill `brain-query`): answer questions or draft documents from vault content only, citing notes.

## Team rules

- Every member authors notes, so contributions show in commit history (SB-6).
- Brain changes, including AI-made ones, reach `main` only through reviewed pull requests (SB-5).
- At the end of each stage we write a review note: what worked, what did not, what to change (SB-2).
- Significant AI sessions are recorded as `type: ai-session` notes: goal, what worked, what failed (AI-3).
- Every ADR and every skill cites at least one distilled note (SB-3) - for example this one.
