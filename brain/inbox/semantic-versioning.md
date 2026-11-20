---
title: "Semantic Versioning 2.0.0"
type: research
status: inbox
tags: [versioning, release, process]
created: 2026-11-20
author: Manne Rohith Sai
related: [PR-4, "#4", "PR #18"]
---

# Semantic Versioning 2.0.0

## Summary

Semantic Versioning 2.0.0 (semver.org): version numbers of the form MAJOR.MINOR.PATCH whose increments communicate the kind of change. Our SRS requires release tags to use it.

## Key points

- Increment MAJOR for incompatible API changes, MINOR for backward-compatible functionality, PATCH for backward-compatible bug fixes; pre-release and build labels are extensions.
- Software using SemVer must declare a public API (in code or documentation).
- A normal version is X.Y.Z with non-negative integers and no leading zeroes; each element increases numerically (1.9.0 -> 1.10.0).
- Once a version is released its contents must not be modified; any change is a new version.
- Major version zero (0.y.z) is for initial development: anything may change and the public API should not be considered stable. The FAQ suggests starting initial development at 0.1.0 and incrementing the minor version for each release.
- 1.0.0 defines the public API; if software is used in production or has users depending on a stable API it should already be 1.0.0.
- Pre-release versions append a hyphen and dot-separated identifiers (e.g. 1.0.0-alpha.1) and have lower precedence than the release.
- "v1.2.3" is not itself a semantic version, but prefixing "v" is a common way to mark a version number in version control.

## Links

- Used together with [[keep-a-changelog]].
- Team observation (not from the source): following the FAQ, our stage releases can be tagged v0.1.0, v0.2.0, ... and the final Stage 4 release that delivers the SRS's release 1.0 as v1.0.0.

## Requirements

- [[srs-requirements#PR-4|PR-4]]

Issues / PRs: #4, PR #18

## Source

- https://semver.org/spec/v2.0.0.html
- Ingested 2026-11-20 by Manne Rohith Sai with the brain-ingest skill.
