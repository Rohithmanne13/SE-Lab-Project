---
title: "RFC 9457: Problem Details for HTTP APIs"
type: research
status: distilled
tags: [http, errors, api]
created: 2026-11-20
author: Manne Rohith Sai
related: [FR-0.3, UI-3, SEC-6, CON-4, "#4", "PR #18"]
---

# RFC 9457: Problem Details for HTTP APIs

## Summary

RFC 9457 (IETF, obsoletes RFC 7807) defines "problem details": a standard JSON object, media type `application/problem+json`, for carrying machine-readable details of errors in HTTP responses so that APIs do not invent their own error formats.

## Key points

- Standard members: `type` (a URI reference identifying the problem type; consumers use it as the primary identifier; defaults to "about:blank"), `status` (the HTTP status code, advisory only), `title` (short human-readable summary of the problem type, should not change between occurrences), `detail` (explanation specific to this occurrence, focused on helping the client correct the problem rather than debugging) and `instance` (URI reference for this occurrence).
- Absolute `type` URIs are recommended; relative URIs can resolve differently on different resources. Non-resolvable URIs (e.g. tag: URIs) are allowed, but resolvable ones are encouraged.
- Problem types may define extension members (e.g. an `errors` list for validation errors); clients must ignore extensions they do not recognise.
- "about:blank" means the problem has no semantics beyond the HTTP status code, and its title should be the status phrase (e.g. "Not Found").
- Security considerations: vet the information included; avoid exposing implementation details such as stack dumps; `status` duplicates the real status code and can disagree with it if an intermediary changes the code.

## Links

- Our API context: [[project-statement-financial-ledger]].
- Team observation (not from the source): our SRS error model (section 3.3.4) uses exactly these members plus the extensions `code`, `request_id` and `errors`, and SEC-6 forbids stack traces and SQL in responses, consistent with this RFC's security guidance.

## Requirements

- [[srs-requirements#FR-0.3|FR-0.3]]
- [[srs-requirements#UI-3|UI-3]]
- [[srs-requirements#SEC-6|SEC-6]]
- [[srs-requirements#CON-4|CON-4]]

Issues / PRs: #4, PR #18

## Source

- https://www.rfc-editor.org/rfc/rfc9457
- Ingested 2026-11-20 by Manne Rohith Sai with the brain-ingest skill.
