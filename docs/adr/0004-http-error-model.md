# ADR-0004: HTTP error model and request validation

## Status

Accepted (2026-11-23)

## Context

- Every error is an RFC 9457 problem document (`application/problem+json`) with at least `type`, `title`, `status`, `detail`, and the extensions `code` and `request_id`; validation errors add `errors` (a list of `{field, message}`); no stack traces, SQL or file paths (SRS 3.3.4, FR-0.3, UI-3, SEC-6).
- The first failing check determines the error, in the order authenticate -> authorise -> validate headers, path and body -> execute (SRS 4.7.2, FR-0.1, FR-0.2).
- Every parameter is validated for type, format, length and range; unknown body fields are rejected; amounts must be positive JSON integers - fractions, exponents, strings and booleans are rejected (FR-0.2, FR-2.10, SEC-1, SEC-2, CI-2, TQ-6).
- Bodies over 16 KiB get 413; non-JSON POSTs get 415; every response carries `X-Request-ID` (FR-0.4, FR-0.5).

## Decision

1. **One exception type per error code.** Services throw `ApiException(ErrorCode, detail)`; `ErrorCode` is an enum holding every code of SRS 3.3.4 with its HTTP status and title (e.g. `INSUFFICIENT_FUNDS -> 422 "Insufficient funds"`).
2. **One `@RestControllerAdvice`** converts `ApiException`, validation failures, malformed JSON, unsupported media type, oversize bodies and unexpected exceptions into Spring `ProblemDetail` objects:
   - `type`: `https://github.com/Rohithmanne13/SE-Lab-Project/blob/main/docs/design/error-codes.md#<code-in-lowercase>` - an absolute, resolvable URI per problem type;
   - `title`: the fixed title of the code; `status`: the HTTP status; `detail`: one sentence telling the caller what to correct;
   - extensions `code` (e.g. `INSUFFICIENT_FUNDS`) and `request_id`; `errors` for `VALIDATION_ERROR`;
   - unexpected exceptions become 500 `INTERNAL_ERROR` with a generic detail; the stack trace goes only to the log, keyed by `request_id` (SEC-6, SEC-7).
3. **Pipeline order** is fixed by servlet filters and Spring, in this order: `RequestIdFilter` (assigns or echoes `X-Request-ID`, FR-0.4) -> `PayloadSizeFilter` (413 above 16 KiB, FR-0.5) -> `ApiKeyFilter` (401/403, constant-time key comparison, FR-0.1, SEC-4, SEC-5) -> content type (415) -> header checks (`Idempotency-Key`, 400) -> path and body validation (400) -> service (404/409/422/503).
4. **Strict JSON** (FR-0.2, FR-2.10, TQ-6): Jackson is configured with `FAIL_ON_UNKNOWN_PROPERTIES` and with no coercion of strings, floats or booleans to integers (`ACCEPT_FLOAT_AS_INT` off, coercion configs set to `Fail`); amount fields are Java `long` in integer minor units. Request DTOs are Java records with Jakarta Bean Validation annotations (`@NotNull`, `@Positive`, `@Max(9007199254740991)`, `@Size`, `@Pattern`).
5. **Identifiers** in paths and bodies are parsed as canonical UUIDs; a malformed one is 400 `INVALID_ID` before any database access (FR-1.9).
6. The table of codes, statuses, titles and when each is used lives in `docs/design/error-codes.md` and is the target of every `type` URI.

## Alternatives considered

- **Spring's default error body** - not RFC 9457 and leaks exception class names.
- **`about:blank` for every problem** - allowed by RFC 9457 but gives clients no per-code documentation; RFC 9457 encourages resolvable type URIs.
- **Validating money as `BigDecimal`** - would accept fractions and invite floating-point paths; `long` minor units plus strict coercion settings make fractions impossible (CON-3).

## Consequences

- Adding an error means adding one enum constant and one row in `error-codes.md`; tests can assert on the stable `code` instead of messages.
- Strict coercion means clients must send numbers as JSON integers - exactly what CI-2 requires.
- The filter order is part of the contract and is covered by negative tests in Stage 3 (TQ-4, TQ-SEC).

## Sources

- [[rfc-9457-problem-details]] - members of a problem details object, absolute resolvable `type` URIs, extensions, not exposing stack dumps.
- [[idempotency-key-header-draft]] - the 400 and 422 idempotency errors and their problem-details bodies.
- [[srs-requirements]] - section 3.3.4, FR-0.1-FR-0.5, FR-2.10, SEC-1, SEC-2, SEC-6.
