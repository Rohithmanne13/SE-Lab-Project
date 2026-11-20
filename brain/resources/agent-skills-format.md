---
title: "Agent Skills format (how Team 007 writes skills)"
type: concept
status: distilled
tags: [agent-skills, skills, process]
created: 2026-11-20
author: Manne Rohith Sai
related: [SK-0, CON-8, SB-3, TQ-7, PR-5, UI-4, "#2", "PR #18"]
---

# Agent Skills format

Distilled from [[agent-skills-specification]] and [[sdlc-and-workflow]], applied to SK-0 in [[srs-requirements#SK-0|srs-requirements]]. Every skill in `skills/` follows these rules; CI (`scripts/ci/validate_skills.py`) enforces them on every pull request.

## Folder layout

```text
skills/<skill-name>/
├── SKILL.md      # required: front matter + instructions
└── scripts/      # optional: helper scripts the agent runs
```

The skills location is also stated in `AGENTS.md` (SK-0).

## Front matter (Agent Skills specification)

- `name` (required): 1-64 characters, lowercase `a-z`, `0-9` and single hyphens, not starting or ending with a hyphen, identical to the folder name.
- `description` (required): 1-1024 characters saying what the skill does **and** when to use it.
- Optional fields allowed by the specification: `license`, `compatibility` (max 500 characters), `metadata`, `allowed-tools`. No other fields.

## Body (SK-0)

Each SKILL.md has these sections, in this order:

1. `## Inputs` - what the caller must provide.
2. `## Steps` - numbered, step-by-step instructions.
3. `## Outputs` - files changed and the report printed; reports mark each step PASS or FAIL (UI-4).
4. `## Failure handling` - what to do when a step fails; never leave half-written results.
5. `## Evaluation` - how the skill is checked before each stage submission; results go to `docs/testing/skill-evaluations.md` (TQ-7).

Each skill cites at least one distilled brain note with a `[[wikilink]]` (SB-3), keeps SKILL.md under 500 lines, and keeps helper scripts self-contained with clear error messages ([[agent-skills-specification]]).

## Our skills

| Skill | Stage | Requirement |
|---|---|---|
| brain-ingest | 1 | [[srs-requirements#SK-1\|SK-1]] |
| brain-refine | 2 | [[srs-requirements#SK-2\|SK-2]] |
| brain-query | 2 | [[srs-requirements#SK-3\|SK-3]] |
| local-check | 2 | [[srs-requirements#SK-4\|SK-4]] |
| simulate-transfer | 3 | [[srs-requirements#SK-5\|SK-5]] |
