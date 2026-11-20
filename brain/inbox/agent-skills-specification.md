---
title: "Agent Skills format specification"
type: research
status: inbox
tags: [agent-skills, skills, specification]
created: 2026-11-20
author: Manne Rohith Sai
related: [SK-0, CON-8, PR-5, "#4", "PR #18"]
---

# Agent Skills format specification

## Summary

The Agent Skills format specification (agentskills.io), which the course names as the specification for SKILL.md folders. A skill is a directory containing at least a `SKILL.md` file: YAML front matter followed by Markdown instructions, optionally with `scripts/`, `references/` and `assets/` folders.

## Key points

- Required front matter: `name` and `description`. Optional: `license`, `compatibility` (max 500 chars), `metadata` (string-to-string map) and `allowed-tools` (experimental).
- `name`: 1-64 characters; only lowercase a-z, 0-9 and hyphens; must not start or end with a hyphen; no consecutive hyphens; must match the parent directory name.
- `description`: 1-1024 characters; should say both what the skill does and when to use it, with keywords that help agents pick it.
- The body has no format restrictions; recommended content is step-by-step instructions, examples of inputs and outputs, and common edge cases.
- Progressive disclosure: name and description (~100 tokens) are loaded at startup for all skills; the full SKILL.md body (under 5000 tokens recommended; keep under 500 lines) when the skill is activated; files in scripts/, references/ and assets/ only when needed.
- Scripts should be self-contained or document dependencies, give helpful error messages and handle edge cases. File references use relative paths from the skill root, one level deep.
- Validation: the `skills-ref validate ./my-skill` reference tool checks front matter and naming conventions.

## Links

- The course requirement to validate every SKILL.md in CI is in [[sdlc-and-workflow]].

## Requirements

- [[srs-requirements#SK-0|SK-0]]
- [[srs-requirements#CON-8|CON-8]]
- [[srs-requirements#PR-5|PR-5]]

Issues / PRs: #4, PR #18

## Source

- https://agentskills.io/specification
- Ingested 2026-11-20 by Manne Rohith Sai with the brain-ingest skill.
