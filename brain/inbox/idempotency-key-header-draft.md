---
title: "The Idempotency-Key HTTP header field (IETF draft -08)"
type: research
status: inbox
tags: [http, idempotency, api]
created: 2026-11-20
author: Manne Rohith Sai
related: [FR-2, FR-2.3, FR-2.4, FR-2.6, FR-2.7, FR-2.8, FR-2.9, BR-9, "#4", "PR #18"]
---

# The Idempotency-Key HTTP header field (IETF draft -08)

## Summary

IETF Internet-Draft draft-ietf-httpapi-idempotency-key-header-08 (J. Jena, S. Dalal, D. Tishkin; 22 October 2026, expires 25 April 2027). It defines the `Idempotency-Key` HTTP request header, which makes non-idempotent methods such as POST and PATCH fault-tolerant when clients retry after timeouts. It is a draft, not yet an RFC.

## Key points

- Motivation: per RFC 9110, POST and PATCH are not idempotent; a client whose POST timed out cannot tell whether it succeeded or whether a retry is safe.
- The key is a unique client-generated value; syntactically the header is a Structured Field Item whose value MUST be a String (quoted), e.g. `Idempotency-Key: "8e03978e-40d5-43e8-bc93-6894a57f9324"`; a UUID or similar random identifier is RECOMMENDED.
- A key MUST NOT be reused with a different request payload. The resource MAY expire keys and SHOULD publish its expiry policy; resources MUST publish an idempotency specification.
- An idempotency fingerprint MAY be generated from the request payload (e.g. a checksum of the whole payload or selected fields) and used together with the key.
- Enforcement: first request -> process normally; duplicate retried after completion -> respond consistently with the completed request; duplicate arriving while the original is still processing -> respond with a conflict.
- Errors: missing key on an operation that requires it -> 400; key reused with a different payload -> 422 (Unprocessable Content); request retried while the original is still outstanding -> 409; problem details per RFC 9457 or a `Link` header to documentation.

## Links

- Error bodies follow [[rfc-9457-problem-details]]; atomic storage of the key relies on [[sqlite-transactions]].
- Team observations for design (not from the source, SRS unchanged): (1) our FR-2.3/FR-2.7 status codes (400 missing, 422 reused) match this draft; (2) the draft suggests 409 for a duplicate that arrives while the original is in progress, whereas FR-2.8 serialises such requests so they receive the replayed response after commit; (3) the draft's header value is a quoted String, while our SRS key format (8-128 characters of `A-Z a-z 0-9 - _`) is unquoted - worth an ADR note in Stage 2; (4) FR-2.9 (no expiry in release 1.0) is the published expiry policy the draft asks for.

## Requirements

- [[srs-requirements#FR-2|FR-2]]
- [[srs-requirements#FR-2.3|FR-2.3]]
- [[srs-requirements#FR-2.4|FR-2.4]]
- [[srs-requirements#FR-2.6|FR-2.6]]
- [[srs-requirements#FR-2.7|FR-2.7]]
- [[srs-requirements#FR-2.8|FR-2.8]]
- [[srs-requirements#FR-2.9|FR-2.9]]
- [[srs-requirements#BR-9|BR-9]]

Issues / PRs: #4, PR #18

## Source

- https://datatracker.ietf.org/doc/draft-ietf-httpapi-idempotency-key-header/
- Ingested 2026-11-20 by Manne Rohith Sai with the brain-ingest skill.
