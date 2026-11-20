# Contributing

How Team 007 works in this repository. Required by SRS PR-1 (documented branching strategy) and used together with `AGENTS.md`.

## Workflow

1. **Issue first (PR-2).** Every piece of work and every defect is a GitHub Issue. Name the SRS requirement IDs it covers.
2. **Branch** from an up-to-date `main` (naming below).
3. **Commit** in small, meaningful steps (PR-1). Every member commits their own work from their own GitHub account, so contributions are visible in history (SB-6).
4. **Check locally** before pushing:

   ```bash
   python3 scripts/ci/validate_brain.py
   python3 scripts/ci/validate_skills.py
   python3 -m unittest discover -s tests -v
   ```

5. **Open a pull request** to `main`. Write `Closes #<issue>` in the description and list the requirement IDs it satisfies.
6. **Review (PR-3).** At least one approving review from a team member who is not the author. AI-generated changes are reviewed like any other change, and the author must be able to explain every line (AI-1, AI-2).
7. **Merge** only when CI is green and the review is approved. Branch protection on `main` enforces both and blocks direct pushes.
8. **Delete the branch** after merging.

## Branching strategy

- `main` - always releasable; protected; changes only by reviewed pull request.
- Short-lived branches from `main`, one per issue, named `<type>/<short-description>`:

| Type | Use for | Example |
|---|---|---|
| `feature/` | New functionality, skills, deliverables | `feature/transfer-endpoint` |
| `fix/` | Defect fixes (with a regression test, TQ-8) | `fix/idempotency-replay-status` |
| `docs/` | Documentation, ADRs, diagrams | `docs/adr-0001-language` |
| `brain/` | Notes in the `brain/` vault (SB-5) | `brain/ingest-mcp-intro` |
| `ci/` | CI workflow and validators | `ci/enable-maven-build` |
| `test/` | Tests only | `test/concurrency-fr-3-4` |

## Commit messages

```text
<type>: <imperative summary, max ~72 characters>

<what and why; requirement IDs, e.g. "Implements FR-2.3, FR-2.7.">
```

`<type>` is one of `feature`, `fix`, `docs`, `brain`, `ci`, `test`, `chore`.

## Brain notes

Add research with the brain-ingest skill (`skills/brain-ingest/SKILL.md`). Follow `brain/resources/second-brain-workflow.md` for front matter (DR-8) and links (SB-1). Brain changes go through pull requests like code (SB-5).

## Defects (TQ-8)

Open an Issue labelled as a bug with steps to reproduce. The fixing pull request adds a regression test that fails before the fix and passes after it.

## Releases (PR-4)

- Versions follow Semantic Versioning: `v0.1.0`, `v0.2.0`, ... for the stage releases during development, and `v1.0.0` for the final Stage 4 release (SRS release 1.0).
- Before tagging, move the `## [Unreleased]` entries in `CHANGELOG.md` into a new version section with the release date.
- Tag `main` after the release pull request is merged: `git tag -a v0.1.0 -m "Stage 1" && git push origin v0.1.0`.

## Secrets

Never commit secrets or paste them into AI tools (CON-9, AI-4). CI secret scanning fails the build if one is found (SEC-8).
