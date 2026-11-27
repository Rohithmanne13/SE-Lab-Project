# Architecture Decision Records

Significant design decisions, one file each (SRS SE-3). An ADR records a decision the SRS leaves open or how we meet a requirement; it never changes a requirement - the submitted SRS (`docs/srs/`) stays the baseline.

| ADR | Title | Status | Requirements |
|---|---|---|---|
| [0001](0001-implementation-stack.md) | Implementation stack: Java 21, Spring Boot, Maven, SQLite JDBC | Accepted | CON-6, CON-4, CON-1, OE-2, SEC-9 |
| [0002](0002-transactions-concurrency-idempotency.md) | Transactions, concurrency and idempotency on SQLite | Accepted | FR-2, FR-3, SI-2, SI-3, CON-2, NFR-SAFE-3 |
| [0003](0003-ledger-integrity-hash-chain.md) | Ledger integrity: append-only tables and the hash chain | Accepted | FR-4, FR-5, DR-4, BR-4 |
| [0004](0004-http-error-model.md) | HTTP error model and request validation | Accepted | FR-0, UI-3, SEC-1, SEC-6 |

## Rules

- File name `NNNN-short-title.md`, numbered in order; never renumber or delete an ADR. A changed decision gets a new ADR whose Status says which one it supersedes, and the old one's Status becomes `Superseded by ADR-NNNN`.
- Sections, in this order: `## Status`, `## Context`, `## Decision`, `## Alternatives considered`, `## Consequences`, `## Sources`.
- Every ADR cites at least one **distilled** brain note as a `[[note-name]]` link (SB-3); `brain/` is the vault, so the link is checked against it. CI (`scripts/ci/validate_adrs.py`) enforces the sections and the citation.
- Cite requirement IDs from the submitted SRS for everything the decision is meant to satisfy.

## Template

```markdown
# ADR-NNNN: <decision in a few words>

## Status

Proposed | Accepted (YYYY-MM-DD) | Superseded by ADR-NNNN

## Context

<the forces and the requirement IDs that make a decision necessary>

## Decision

<what we will do, stated so a reviewer can check the code against it>

## Alternatives considered

<options rejected, and why>

## Consequences

<what becomes easier or harder; follow-up work>

## Sources

- [[distilled-note-name]] - what it contributed
```
