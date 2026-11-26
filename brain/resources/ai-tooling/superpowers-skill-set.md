---
title: "Superpowers skill set for coding agents"
type: research
status: distilled
tags: [agent-skills, ai-tools]
created: 2026-11-26
author: Rudraraju Dhanush Varma
related: [SK-0, TQ-7, AI-1, PR-3, "#14"]
---

# Superpowers skill set for coding agents

## Summary

Superpowers (github.com/obra/superpowers, MIT License) is the skill set the course names in its SDLC document for coding assistants. Its README describes it as a complete software development methodology for coding agents, built on composable skills plus initial instructions that make the agent use them. It is built by Jesse Vincent and Prime Radiant and installs as a plugin in many agents, including Claude Code, Codex, Cursor and GitHub Copilot CLI.

## Key points

- Superpowers is a complete software development methodology for coding agents, built on top of a set of composable skills and some initial instructions that make sure the agent uses them.
- The agent does not jump straight into writing code: it first asks what you are really trying to do, then shows the resulting spec in chunks short enough to read and digest.
- After the design is signed off, the agent writes an implementation plan that emphasizes true red/green TDD, YAGNI (You Aren't Gonna Need It) and DRY, then works through it with a subagent-driven-development process that inspects and reviews each task.
- The basic workflow is seven skills: brainstorming, using-git-worktrees, writing-plans, subagent-driven-development or executing-plans, test-driven-development, requesting-code-review, and finishing-a-development-branch.
- In writing-plans, work is broken into bite-sized tasks of 2-5 minutes, each with exact file paths, complete code and verification steps.
- test-driven-development enforces RED-GREEN-REFACTOR: write a failing test, watch it fail, write minimal code, watch it pass, commit; code written before tests is deleted.
- requesting-code-review runs between tasks, reviews against the plan and reports issues by severity; critical issues block progress.
- The agent checks for relevant skills before any task; the workflows are mandatory, not suggestions. The stated philosophy: test-driven development, systematic over ad-hoc, complexity reduction, evidence over claims.

## Links

- The course lists Superpowers among its tools and references in [[sdlc-and-workflow]] (section 9).
- Each Superpowers skill is an Agent Skill as described in [[agent-skills-specification]].
- Team observation (not from the source): its test-first and review-before-merge practices match our SRS requirements for tests per requirement (TQ-1 to TQ-8) and a reviewed pull request for every change (PR-3, AI-1).

## Requirements

- [[srs-requirements#SK-0|SK-0]]
- [[srs-requirements#TQ-7|TQ-7]]
- [[srs-requirements#AI-1|AI-1]]
- [[srs-requirements#PR-3|PR-3]]

Issues / PRs: #14

## Source

- https://github.com/obra/superpowers
- Ingested 2026-11-26 by Rudraraju Dhanush Varma with the brain-ingest skill.
