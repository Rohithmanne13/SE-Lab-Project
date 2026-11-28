# Team 007 second brain - index

Entry point of our Obsidian vault for the **Financial Ledger & Digital Wallet API** (SE Lab, IIIT Bangalore).
Open the `brain/` folder in Obsidian ("Open folder as vault"); no account is needed.

- Requirements baseline: [[srs-requirements]] (every SRS requirement ID, verbatim from the submitted SRS).
- How this vault works: [[second-brain-workflow]].
- Every skill run is recorded in [[log]].

## How the vault is organised (PARA, SB-0)

| Folder | Holds |
|---|---|
| `inbox/` | Unsorted new notes, created by the brain-ingest skill (status `inbox`). |
| `projects/` | Material for active, deadline-driven work - our project `projects/financial-ledger/`. |
| `areas/` | Ongoing responsibilities without an end date. |
| `resources/` | Distilled reference material reused across work. |
| `archive/` | Inactive notes from the other folders. |

Every note starts with the front matter required by DR-8 (`title`, `type`, `status`, `tags`, `created`, `author`, `related`), and links to related notes and requirement IDs with `[[wikilinks]]` (SB-1). Changes reach `main` only through reviewed pull requests (SB-5).

## Projects

### financial-ledger

- [[srs-requirements]] - SRS requirement index (all 179 IDs).
- [[stage-1-review]] - Stage 1 review note (SB-2).
- [[stage-2-review]] - Stage 2 review note (SB-2).
- [[stage-3-review]] - Stage 3 review note (SB-2).
- [[ai-session-2026-11-28-stage-3-build]] - AI session: preparing the Stage 3 deliverables (AI-3).
- [[ai-session-2026-11-23-stage-2-preparation]] - AI session: preparing the Stage 2 deliverables (AI-3).
- [[ai-session-2026-11-11-srs-condensing]] - AI session: condensing the SRS to 15 pages (AI-3).
- [[ai-session-2026-11-20-stage-1-setup]] - AI session: setting up the Stage 1 deliverables (AI-3).
- [[project-statement-financial-ledger]] - Project statement: Financial Ledger & Digital Wallet API (2026-11-20, Manne Rohith Sai)
- [[sdlc-and-workflow]] - The AI-Assisted SDLC and workflow (course document) (2026-11-20, Manne Rohith Sai)
- [[ieee-srs-template]] - IEEE SRS template (Wiegers, 1999) (2026-11-20, Manne Rohith Sai)

## Areas

### second-brain

- [[para-method]] - The PARA method (Tiago Forte) (2026-11-20, Manne Rohith Sai)
- [[obsidian-internal-links]] - Obsidian internal links (2026-11-20, Manne Rohith Sai)

### release-management

- [[keep-a-changelog]] - Keep a Changelog 1.1.0 (2026-11-20, Manne Rohith Sai)
- [[semantic-versioning]] - Semantic Versioning 2.0.0 (2026-11-20, Manne Rohith Sai)

## Resources

- [[second-brain-workflow]] - how we run the vault: PARA, index, log, note schema, links, the three brain skills.
- [[agent-skills-format]] - how our Agent Skills are written and validated.

### sqlite

- [[sqlite-transactions]] - SQLite transactions (2026-11-20, Manne Rohith Sai)
- [[sqlite-write-ahead-logging]] - SQLite write-ahead logging (WAL) (2026-11-20, Manne Rohith Sai)
- [[sqlite-file-locking]] - SQLite file locking and concurrency (2026-11-20, Manne Rohith Sai)

### http-api

- [[rfc-9457-problem-details]] - RFC 9457: Problem Details for HTTP APIs (2026-11-20, Manne Rohith Sai)
- [[idempotency-key-header-draft]] - The Idempotency-Key HTTP header field (IETF draft -08) (2026-11-20, Manne Rohith Sai)
- [[openapi-specification-3-1]] - OpenAPI Specification 3.1.0 (2026-11-26, Abhiram P B S)

### ai-tooling

- [[agent-skills-specification]] - Agent Skills format specification (2026-11-20, Manne Rohith Sai)
- [[model-context-protocol-intro]] - Model Context Protocol introduction (2026-11-23, Sunnapu Suhith Reddy)
- [[superpowers-skill-set]] - Superpowers skill set for coding agents (2026-11-26, Rudraraju Dhanush Varma)

## Inbox

Maintained by the brain-ingest skill; brain-refine (Stage 2) moves these notes into PARA folders.

_(none yet)_

## Archive

_(none yet)_
