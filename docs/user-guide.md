# API usage guide

Step-by-step walkthrough of FR-1 to FR-5 with cURL (SRS 2.6). The full contract - every endpoint, schema, header,
status and error code - is `docs/api/openapi.yaml`, also shown as Swagger UI at <http://127.0.0.1:8000/docs> while the
service runs. The same steps are ready to run as a Postman collection: `docs/api/financial-ledger.postman_collection.json`.

The outputs below are real responses from one run; your IDs, hashes and times will differ.

## 0. Start the service and set your keys

The service reads its API keys from environment variables (CON-9, SEC-5) - at least 32 random characters each.
In a WSL/Linux/macOS terminal, from the repository root:

```bash
export LEDGER_CLIENT_KEYS="$(python3 -c 'import secrets; print(secrets.token_urlsafe(32))')"
export LEDGER_ADMIN_KEYS="$(python3 -c 'import secrets; print(secrets.token_urlsafe(32))')"
sh ./mvnw -B -q -DskipTests package
java -jar target/financial-ledger-api.jar
```

Keep that terminal open. In a second terminal, copy the two keys from the first one (`echo $LEDGER_CLIENT_KEYS`) and set:

```bash
export B=http://127.0.0.1:8000
export CLIENT="<the LEDGER_CLIENT_KEYS value>"
export ADMIN="<the LEDGER_ADMIN_KEYS value>"
curl -s $B/health
```

```json
{"status":"ok","database":"ok","version":"0.3.0"}
```

Rules worth knowing before you start:

- Money is a JSON **integer** of minor units: `50000` = INR 500.00, `12550` = INR 125.50. `10.5`, `1e3`, `"100"`
  and `true` are rejected (CI-2, TQ-6).
- Every request except `/health` and `/docs` needs `X-API-Key`. Deposits, reversals and reconciliation need the
  ADMIN key (FR-0.1, SEC-4).
- Deposits, transfers and reversals need an `Idempotency-Key` header - 8-128 characters of `A-Z a-z 0-9 - _`; a new
  UUID (`python3 -c 'import uuid; print(uuid.uuid4())'`) is a good key (FR-2.3).
- Errors are RFC 9457 problem documents with a stable `code`, a `detail` saying what to correct and the
  `request_id` (SRS 3.3.4); `docs/design/error-codes.md` lists every code.

## 1. Wallet initialisation (FR-1)

Create a user (FR-1.1). The name is trimmed; the email must be unique ignoring case (FR-1.2):

```bash
curl -s -X POST $B/api/v1/users -H "X-API-Key: $CLIENT" -H "Content-Type: application/json" \
  -d '{"name":"Alice Example","email":"alice@example.com"}'
```

```json
{
  "user_id": "5cb4d9b9-e4a1-40d1-bf4d-aa37f76f721f",
  "name": "Alice Example",
  "email": "alice@example.com",
  "created_at": "2026-11-28T04:01:51.330Z"
}
```

Create Bob the same way (`bob@example.com`). Then give each one an INR wallet (FR-1.3) - it starts at 0:

```bash
export ALICE_USER=5cb4d9b9-e4a1-40d1-bf4d-aa37f76f721f      # use your own IDs
curl -s -X POST $B/api/v1/users/$ALICE_USER/wallets -H "X-API-Key: $CLIENT" -H "Content-Type: application/json" \
  -d '{"currency":"INR"}'
```

```json
{
  "wallet_id": "5d06c777-c4c8-48f2-b437-2486b54d2fe4",
  "user_id": "5cb4d9b9-e4a1-40d1-bf4d-aa37f76f721f",
  "currency": "INR",
  "balance": 0,
  "created_at": "2026-11-28T04:01:51.564Z"
}
```

A second INR wallet for the same user is refused (FR-1.4); `USD` would be allowed:

```json
{
  "type": "https://github.com/Rohithmanne13/SE-Lab-Project/blob/main/docs/design/error-codes.md#wallet_already_exists",
  "title": "Wallet already exists",
  "status": 409,
  "detail": "This user already has a wallet in INR; a user has one wallet per currency.",
  "code": "WALLET_ALREADY_EXISTS",
  "request_id": "8ff8e9ec-c974-4b17-aaef-84ea986085c4"
}
```

Money enters only through an administrative deposit (FR-1.6): one transaction that DEBITs the INR system funding
account and CREDITs Alice's wallet. The system account's ID is never shown (`wallet_id: null`, FR-1.5):

```bash
export ALICE=5d06c777-c4c8-48f2-b437-2486b54d2fe4
export BOB=0607832b-ef64-4c0b-bec0-2a0211e1fa33
export DEPOSIT_KEY=$(python3 -c 'import uuid; print(uuid.uuid4())')        # a new Idempotency-Key for this deposit
curl -s -X POST $B/api/v1/wallets/$ALICE/deposits -H "X-API-Key: $ADMIN" -H "Content-Type: application/json" \
  -H "Idempotency-Key: $DEPOSIT_KEY" \
  -d '{"amount":50000,"currency":"INR","description":"Initial funding"}'
```

```json
{
  "transaction_id": "9188dbbc-b94f-49f1-b91d-b8b30441f877",
  "type": "DEPOSIT",
  "status": "COMPLETED",
  "source_wallet_id": null,
  "destination_wallet_id": "5d06c777-c4c8-48f2-b437-2486b54d2fe4",
  "amount": 50000,
  "currency": "INR",
  "description": "Initial funding",
  "reverses_transaction_id": null,
  "reversed_by_transaction_id": null,
  "created_at": "2026-11-28T04:01:51.814Z",
  "entries": [
    {"entry_id": "9b07298d-...", "sequence": 1, "wallet_id": null, "direction": "DEBIT", "amount": 50000,
     "prev_hash": "0000000000000000000000000000000000000000000000000000000000000000",
     "entry_hash": "dfb9680abc32ff96e4a3f71f613b5e6082a434f06629224a6988afaef06cf6db", "...": "..."},
    {"entry_id": "27afe728-...", "sequence": 2, "wallet_id": "5d06c777-c4c8-48f2-b437-2486b54d2fe4",
     "direction": "CREDIT", "amount": 50000,
     "prev_hash": "dfb9680abc32ff96e4a3f71f613b5e6082a434f06629224a6988afaef06cf6db",
     "entry_hash": "9a26216b9ab7aa2d98dd2bd2bf5da17509a5e7f0030d292fb2b67db81749f145", "...": "..."}
  ]
}
```

A CLIENT key here gets `403 FORBIDDEN`.

## 2. Idempotent transfers (FR-2)

Send INR 125.50 from Alice to Bob with a new key (FR-2.1, FR-2.2):

```bash
export TRANSFER_KEY=$(python3 -c 'import uuid; print(uuid.uuid4())')       # keep it: the retry below reuses it
curl -s -i -X POST $B/api/v1/transfers -H "X-API-Key: $CLIENT" -H "Content-Type: application/json" \
  -H "Idempotency-Key: $TRANSFER_KEY" \
  -d "{\"source_wallet_id\":\"$ALICE\",\"destination_wallet_id\":\"$BOB\",\"amount\":12550,\"currency\":\"INR\",\"description\":\"Dinner\"}"
```

```text
HTTP/1.1 201
X-Request-ID: b85cbecd-040c-46c6-964d-47272b12254e
Location: /api/v1/transactions/85be1e61-c51a-447b-bf5e-73ea8d6f49f6
Content-Type: application/json;charset=UTF-8

{"transaction_id":"85be1e61-c51a-447b-bf5e-73ea8d6f49f6","type":"TRANSFER","status":"COMPLETED",
 "source_wallet_id":"5d06c777-...","destination_wallet_id":"0607832b-...","amount":12550,"currency":"INR",
 "description":"Dinner", ... "entries":[{... "direction":"DEBIT" ...},{... "direction":"CREDIT" ...}]}
```

Run **exactly the same command again** (same `$TRANSFER_KEY`) - as a client would after a network timeout. Nothing is posted twice; the
stored response comes back with `Idempotent-Replayed: true` (FR-2.6, CI-4):

```text
HTTP/1.1 201
X-Request-ID: 4d126e14-a5b6-4841-86fa-c31229c48156
Location: /api/v1/transactions/85be1e61-c51a-447b-bf5e-73ea8d6f49f6
Idempotent-Replayed: true
```

The same key with a different body is refused and changes nothing (FR-2.7):

```json
{
  "status": 422,
  "detail": "This Idempotency-Key was already used for a different request (method, path or body); use a new key for a new request.",
  "code": "IDEMPOTENCY_KEY_REUSED",
  "...": "..."
}
```

Other refusals (FR-2.10) - each changes nothing and stores no key, so the client may retry with the same key:

```json
{"status": 422, "code": "INSUFFICIENT_FUNDS",
 "detail": "Wallet 0607832b-ef64-4c0b-bec0-2a0211e1fa33 holds 12550 but the transfer needs 600000; nothing was changed."}
```

```json
{"status": 400, "code": "VALIDATION_ERROR", "detail": "A field of the request body has the wrong type.",
 "errors": [{"field": "amount", "message": "must be a JSON integer (no quotes, fraction or exponent)"}]}
```

Balances now (FR-6.1) - exactly the worked example of SRS Appendix B:

```bash
curl -s $B/api/v1/wallets/$ALICE -H "X-API-Key: $CLIENT"    # "balance": 37450
curl -s $B/api/v1/wallets/$BOB -H "X-API-Key: $CLIENT"      # "balance": 12550
```

## 3. Concurrent balance mutability (FR-3)

Simultaneous transfers from one wallet are executed one at a time by the database write lock (`BEGIN IMMEDIATE`),
so a balance is never overdrawn or overwritten. Try it - fire two transfers of INR 300.00 from Alice (who holds
374.50) at the same moment; exactly one succeeds:

```bash
for i in 1 2; do
  curl -s -o /dev/null -w "%{http_code}\n" -X POST $B/api/v1/transfers -H "X-API-Key: $CLIENT" \
    -H "Content-Type: application/json" -H "Idempotency-Key: $(python3 -c 'import uuid; print(uuid.uuid4())')" \
    -d "{\"source_wallet_id\":\"$ALICE\",\"destination_wallet_id\":\"$BOB\",\"amount\":30000,\"currency\":\"INR\"}" &
done; wait
```

One line prints `201`, the other `422` (`INSUFFICIENT_FUNDS`). The automated acceptance tests of FR-3.4 (20 x 100 from
1000) and FR-3.5 (2 x 600 from 1000) are in `ConcurrencyApiTest`; the simulate-transfer skill runs the same check.

## 4. Immutable audit trail (FR-4)

The ledger history of a wallet, oldest first, paginated (FR-6.2) - here the second of Alice's three entries:

```bash
curl -s "$B/api/v1/wallets/$ALICE/entries?limit=1&offset=1" -H "X-API-Key: $CLIENT"
```

```json
{
  "wallet_id": "5d06c777-c4c8-48f2-b437-2486b54d2fe4",
  "entries": [
    {"entry_id": "e3b08916-...", "sequence": 3, "transaction_id": "85be1e61-...", "direction": "DEBIT",
     "amount": 12550, "prev_hash": "9a26216b...", "entry_hash": "a98eeac2...", "...": "..."}
  ],
  "total_count": 3,
  "limit": 1,
  "offset": 1
}
```

Each `entry_hash` chains to the previous entry's hash (ADR-0003), so a changed row is detected. There is no update
or delete: `curl -X DELETE $B/api/v1/transactions/<id>` gives `405`. A mistake is corrected only by a reversal
(ADMIN, FR-4.5):

```bash
export TRANSFER=85be1e61-c51a-447b-bf5e-73ea8d6f49f6
export REVERSAL_KEY=$(python3 -c 'import uuid; print(uuid.uuid4())')
curl -s -X POST $B/api/v1/transactions/$TRANSFER/reversal -H "X-API-Key: $ADMIN" -H "Content-Type: application/json" \
  -H "Idempotency-Key: $REVERSAL_KEY" -d '{"reason":"Customer dispute"}'
```

```json
{
  "transaction_id": "d51ad7ac-e7e3-4160-ba62-ba97fae8a6e9",
  "type": "REVERSAL",
  "status": "COMPLETED",
  "source_wallet_id": "0607832b-ef64-4c0b-bec0-2a0211e1fa33",
  "destination_wallet_id": "5d06c777-c4c8-48f2-b437-2486b54d2fe4",
  "amount": 12550,
  "currency": "INR",
  "description": "Customer dispute",
  "reverses_transaction_id": "85be1e61-c51a-447b-bf5e-73ea8d6f49f6",
  "reversed_by_transaction_id": null,
  "created_at": "2026-11-28T04:01:52.256Z",
  "entries": ["..."]
}
```

`GET $B/api/v1/transactions/$TRANSFER` now shows `"reversed_by_transaction_id": "d51ad7ac-..."` (FR-4.7). Reversing it
again is `409 TRANSACTION_ALREADY_REVERSED`; reversing the reversal is `422 TRANSACTION_NOT_REVERSIBLE` (FR-4.6).

## 5. Ledger reconciliation (FR-5)

ADMIN only (FR-5.7). It reads one consistent snapshot, checks every rule of FR-5.2-FR-5.5 and returns 200 with the
report - also when the status is FAILED (FR-5.6):

```bash
curl -s $B/api/v1/admin/reconciliation -H "X-API-Key: $ADMIN"
```

```json
{
  "status": "PASSED",
  "checked_at": "2026-11-28T04:03:21.500Z",
  "transactions_checked": 4,
  "entries_checked": 8,
  "wallets_checked": 4,
  "total_debits": 105100,
  "total_credits": 105100,
  "totals": [
    {"currency": "INR", "total_debits": 105100, "total_credits": 105100}
  ],
  "discrepancies": []
}
```

These are the numbers after every step of this guide: 4 transactions (deposit, transfer, the one concurrent transfer
that succeeded, reversal) with 8 entries. `wallets_checked` includes the INR and USD system funding accounts. If a row were changed directly in the database
file, the status would be `FAILED` and `discrepancies` would name the check (for example `ENTRY_HASH`,
`WALLET_BALANCE`), the affected ID and the expected and actual values (see `TamperDetectionApiTest`).

## 6. Where to go next

- Run everything at once: `python3 skills/simulate-transfer/scripts/simulate_transfer.py` (SK-5).
- Check your machine: `python3 skills/local-check/scripts/local_check.py --by "<your full name>"` (SK-4).
- Configuration variables: `README.md` and `.env.example`.
