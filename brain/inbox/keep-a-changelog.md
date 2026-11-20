---
title: "Keep a Changelog 1.1.0"
type: research
status: inbox
tags: [changelog, release, process]
created: 2026-11-20
author: Manne Rohith Sai
related: [PR-4]
---

# Keep a Changelog 1.1.0

## Summary

"Keep a Changelog" version 1.1.0 (keepachangelog.com): a convention for a human-readable `CHANGELOG.md` that records notable changes per released version. The course requires release tags and a changelog.

## Key points

- A changelog is a file with a curated, chronologically ordered list of notable changes for each version; it is for humans, not machines - do not dump git logs into it.
- Guiding principles: an entry for every version; group the same types of change; versions and sections should be linkable; latest version first; show each release date; state whether the project follows Semantic Versioning.
- Types of change: Added, Changed, Deprecated, Removed, Fixed, Security.
- Keep an `## [Unreleased]` section at the top for upcoming changes; at release time move them into a new version section.
- Example header: "All notable changes to this project will be documented in this file. The format is based on Keep a Changelog, and this project adheres to Semantic Versioning." Version headings look like `## [1.1.2] - 2024-09-27`.

## Links

- The course's GitHub requirements (release tags and changelog) are in [[sdlc-and-workflow]].

## Requirements

- [[srs-requirements#PR-4|PR-4]]

## Source

- https://keepachangelog.com/en/1.1.0/
- Ingested 2026-11-20 by Manne Rohith Sai with the brain-ingest skill.
