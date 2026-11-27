# Error codes

Catalogue of every error the API returns (SRS 3.3.4, FR-0.3, UI-3). Each error response is an RFC 9457 problem document whose `type` is a link to the matching heading below, e.g. `https://github.com/Rohithmanne13/SE-Lab-Project/blob/main/docs/design/error-codes.md#insufficient_funds` ([ADR-0004](../adr/0004-http-error-model.md)).

Example:

```json
{
  "type": "https://github.com/Rohithmanne13/SE-Lab-Project/blob/main/docs/design/error-codes.md#insufficient_funds",
  "title": "Insufficient funds",
  "status": 422,
  "detail": "Wallet 3f0c...e21 holds 400 but the transfer needs 600.",
  "code": "INSUFFICIENT_FUNDS",
  "request_id": "6b1f2c4e-6d5a-4b8e-9c1f-2a7d3e9b0c11"
}
```

| HTTP | Code | Title |
|---|---|---|
| 400 | `VALIDATION_ERROR` | Invalid request |
| 400 | `INVALID_ID` | Invalid identifier |
| 400 | `IDEMPOTENCY_KEY_MISSING` | Idempotency-Key is missing |
| 400 | `IDEMPOTENCY_KEY_INVALID` | Idempotency-Key is malformed |
| 401 | `UNAUTHENTICATED` | Authentication required |
| 403 | `FORBIDDEN` | Not allowed |
| 404 | `USER_NOT_FOUND` | User not found |
| 404 | `WALLET_NOT_FOUND` | Wallet not found |
| 404 | `TRANSACTION_NOT_FOUND` | Transaction not found |
| 409 | `EMAIL_ALREADY_EXISTS` | Email already registered |
| 409 | `WALLET_ALREADY_EXISTS` | Wallet already exists |
| 409 | `TRANSACTION_ALREADY_REVERSED` | Transaction already reversed |
| 413 | `PAYLOAD_TOO_LARGE` | Request body too large |
| 415 | `UNSUPPORTED_MEDIA_TYPE` | Unsupported media type |
| 422 | `INSUFFICIENT_FUNDS` | Insufficient funds |
| 422 | `CURRENCY_MISMATCH` | Currency mismatch |
| 422 | `SAME_WALLET_TRANSFER` | Same source and destination |
| 422 | `UNSUPPORTED_CURRENCY` | Unsupported currency |
| 422 | `TRANSACTION_NOT_REVERSIBLE` | Transaction cannot be reversed |
| 422 | `IDEMPOTENCY_KEY_REUSED` | Idempotency-Key already used |
| 422 | `AMOUNT_LIMIT_EXCEEDED` | Amount limit exceeded |
| 500 | `INTERNAL_ERROR` | Internal error |
| 503 | `SERVICE_BUSY` | Service busy |

## VALIDATION_ERROR

400. Body is not valid JSON, a required field is missing, a field has the wrong type or format, an unknown field is present, or a value is out of range - including an amount that is zero, negative, fractional, in exponent notation, a string or a boolean, an invalid currency format, or invalid paging parameters. The `errors` list names each field and what is wrong (FR-0.2, FR-2.10, FR-6.2, SEC-1, SEC-2).

## INVALID_ID

400. A path or body identifier is not a well-formed UUID (FR-1.9, FR-2.10).

## IDEMPOTENCY_KEY_MISSING

400. A `POST` to `/transfers`, `/wallets/{walletId}/deposits` or `/transactions/{transactionId}/reversal` has no `Idempotency-Key` header. Nothing is written; retry with a key (FR-2.3).

## IDEMPOTENCY_KEY_INVALID

400. The `Idempotency-Key` is not 8-128 characters of `A-Z a-z 0-9 - _` (FR-2.3, SRS 3.3.3).

## UNAUTHENTICATED

401. `X-API-Key` is missing or not a configured key (FR-0.1).

## FORBIDDEN

403. A valid CLIENT key was used on an ADMIN endpoint - deposit, reversal or reconciliation (SEC-4, FR-5.7).

## USER_NOT_FOUND

404. A well-formed user ID that does not exist (FR-1.9, FR-6.1).

## WALLET_NOT_FOUND

404. A well-formed wallet ID that does not exist, or a system wallet requested through a CLIENT endpoint (FR-1.5, FR-2.10).

## TRANSACTION_NOT_FOUND

404. A well-formed transaction ID that does not exist (FR-6.3, FR-4.5).

## EMAIL_ALREADY_EXISTS

409. A user with the same email, compared case-insensitively, already exists (FR-1.2).

## WALLET_ALREADY_EXISTS

409. The user already has a wallet in that currency (FR-1.4).

## TRANSACTION_ALREADY_REVERSED

409. The transaction has already been reversed (FR-4.6).

## PAYLOAD_TOO_LARGE

413. The request body is larger than 16 KiB; it was not processed (FR-0.5).

## UNSUPPORTED_MEDIA_TYPE

415. A `POST` without `Content-Type: application/json`.

## INSUFFICIENT_FUNDS

422. The debit would make a non-system wallet balance negative - for a transfer, or for a reversal (FR-2.10, FR-4.6, NFR-SAFE-2).

## CURRENCY_MISMATCH

422. The request currency differs from a wallet's currency, or the two wallets have different currencies (FR-2.10, BR-2).

## SAME_WALLET_TRANSFER

422. Source and destination wallet are the same (FR-2.10).

## UNSUPPORTED_CURRENCY

422. A well-formed ISO 4217 code that is not in the configured supported set (FR-1.3, FR-1.9).

## TRANSACTION_NOT_REVERSIBLE

422. An attempt to reverse a REVERSAL transaction (FR-4.6, BR-7).

## IDEMPOTENCY_KEY_REUSED

422. The `Idempotency-Key` was already used with a different request - method, path or body (FR-2.7, BR-9).

## AMOUNT_LIMIT_EXCEEDED

422. The operation would make a balance exceed 9 007 199 254 740 991 minor units (NFR-REL-6).

## INTERNAL_ERROR

500. An unexpected fault. The transaction was rolled back and nothing was persisted; the cause is logged under the response's `request_id`, never returned (SEC-6).

## SERVICE_BUSY

503. The database write lock could not be acquired within the busy timeout (5000 ms). Nothing was persisted; retry after the number of seconds in `Retry-After` (FR-3.6).
