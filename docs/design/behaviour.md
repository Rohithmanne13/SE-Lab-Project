# Behaviour

UML sequence and state diagrams for every feature of SRS section 4 (SE-2). They refine SRS Figures 3 and 4 into the components of [architecture.md](architecture.md); transaction rules come from [ADR-0002](../adr/0002-transactions-concurrency-idempotency.md), integrity rules from [ADR-0003](../adr/0003-ledger-integrity-hash-chain.md), error handling from [ADR-0004](../adr/0004-http-error-model.md).

## 1. Request pipeline (FR-0)

Every request passes the same checks in a fixed order; the first failing check decides the error (SRS 4.7.2).

```mermaid
sequenceDiagram
    autonumber
    participant C as Client
    participant RF as RequestIdFilter
    participant PF as PayloadSizeFilter
    participant AF as ApiKeyFilter
    participant CT as Controller (validation)
    participant S as Service
    participant H as ApiExceptionHandler
    C->>RF: HTTP request
    RF->>RF: take or create X-Request-ID (FR-0.4)
    RF->>PF: forward
    alt body > 16 KiB
        PF-->>C: 413 PAYLOAD_TOO_LARGE (FR-0.5)
    end
    PF->>AF: forward
    alt key missing or unknown
        AF-->>C: 401 UNAUTHENTICATED
    else CLIENT key on ADMIN endpoint
        AF-->>C: 403 FORBIDDEN (SEC-4)
    end
    AF->>CT: forward (role known)
    CT->>CT: content type, Idempotency-Key, path IDs, body schema (FR-0.2)
    alt invalid
        CT->>H: validation failure
        H-->>C: 400 / 415 problem+json with code and errors
    end
    CT->>S: call with validated input
    S-->>CT: result or ApiException(code)
    CT-->>C: 2xx JSON, or H maps the ApiException to problem+json
```

## 2. Create user and wallet (FR-1.1-FR-1.4)

```mermaid
sequenceDiagram
    autonumber
    participant C as Client
    participant UC as UserController / WalletController
    participant US as UserService / WalletService
    participant R as Repositories
    participant DB as SQLite
    C->>UC: POST /users {name, email}
    UC->>US: createUser(name, email)
    US->>R: insert user (UUID from IdGenerator, time from Clock)
    R->>DB: INSERT INTO users ...
    alt email exists (NOCASE UNIQUE)
        DB-->>US: constraint violation
        US-->>C: 409 EMAIL_ALREADY_EXISTS
    else new
        US-->>C: 201 user + Location (CI-3)
    end
    C->>UC: POST /users/{userId}/wallets {currency}
    UC->>US: createWallet(userId, currency)
    alt user unknown
        US-->>C: 404 USER_NOT_FOUND
    else currency not supported
        US-->>C: 422 UNSUPPORTED_CURRENCY
    else wallet in that currency exists
        US-->>C: 409 WALLET_ALREADY_EXISTS
    else
        US->>DB: INSERT INTO wallets (balance 0)
        US-->>C: 201 wallet (balance 0) + Location
    end
```

## 3. Idempotent, concurrency-safe transfer (FR-2, FR-3)

Deposits (FR-1.6) and reversals (FR-4.5) use the same unit of work with different entries.

```mermaid
sequenceDiagram
    autonumber
    participant C as Client
    participant TC as TransferController
    participant TS as TransferService
    participant TR as TransactionRunner
    participant IS as IdempotencyService
    participant PS as PostingService
    participant DB as SQLite
    C->>TC: POST /transfers, Idempotency-Key K, body B
    TC->>TC: validate key format and body (400)
    TC->>TS: transfer(K, B)
    TS->>IS: fingerprint = SHA-256(POST, path, canonical B) (FR-2.4)
    TS->>TR: inWriteTransaction(...)
    TR->>DB: BEGIN IMMEDIATE (waits <= 5000 ms for the write lock)
    alt lock not obtained
        DB-->>TS: SQLITE_BUSY
        TS-->>C: 503 SERVICE_BUSY + Retry-After (FR-3.6)
    end
    TS->>IS: lookup(K)
    alt K recorded, same fingerprint
        IS-->>TS: stored status + body
        TR->>DB: ROLLBACK (nothing written)
        TS-->>C: same 201 body + Idempotent-Replayed: true (FR-2.6)
    else K recorded, different fingerprint
        TR->>DB: ROLLBACK
        TS-->>C: 422 IDEMPOTENCY_KEY_REUSED (FR-2.7)
    else K new
        TS->>DB: SELECT both wallets (balance read inside the lock, FR-3.2)
        alt wallet missing / same wallet / currency differs / balance < amount
            TR->>DB: ROLLBACK
            TS-->>C: 404 / 422 (FR-2.10)
        else valid
            TS->>PS: post TRANSFER: DEBIT source, CREDIT destination
            PS->>DB: next sequence, last hash, INSERT transaction + 2 hash-chained entries
            PS->>DB: UPDATE both wallet balances
            PS->>PS: assert sum DEBIT = sum CREDIT (FR-1.8)
            TS->>IS: record(K, fingerprint, txn, 201, body) (FR-2.5)
            IS->>DB: INSERT INTO idempotency_records
            TR->>DB: COMMIT (synchronous = FULL, NFR-SAFE-4)
            TS-->>C: 201 transaction + entries + Location
        end
    end
```

## 4. Two concurrent transfers from one wallet (FR-3.4, FR-3.5)

The write lock turns simultaneous requests into a serial order; the second sees the first one's committed balance.

```mermaid
sequenceDiagram
    autonumber
    participant A as Request A (600)
    participant B as Request B (600)
    participant DB as SQLite (balance 1000)
    A->>DB: BEGIN IMMEDIATE - gets the write lock
    B->>DB: BEGIN IMMEDIATE - waits (busy timeout)
    A->>DB: read balance 1000 >= 600 -> post entries, balance 400
    A->>DB: COMMIT - releases the lock
    A-->>A: 201 Created
    DB-->>B: lock granted
    B->>DB: read balance 400 < 600
    B->>DB: ROLLBACK
    B-->>B: 422 INSUFFICIENT_FUNDS
    Note over DB: final balance 400 - exactly one success (FR-3.5)
```

## 5. Life cycle of a money-moving request (FR-2, FR-3, FR-4)

```mermaid
stateDiagram-v2
    [*] --> Received
    Received --> Rejected_no_db : 400 / 401 / 403 / 413 / 415
    Received --> Validated : auth + schema OK
    Validated --> WaitingForLock : BEGIN IMMEDIATE
    WaitingForLock --> Rejected_rolled_back : 503 SERVICE_BUSY after 5000 ms
    WaitingForLock --> Posting : write lock acquired
    Posting --> Replayed : key seen, same fingerprint
    Posting --> Rejected_rolled_back : 404 / 409 / 422 / 500
    Posting --> Completed : COMMIT -> 201
    Replayed --> [*] : stored 201 + Idempotent-Replayed
    Completed --> [*]
    Rejected_no_db --> [*]
    Rejected_rolled_back --> [*]
```

## 6. Life cycle of a ledger transaction (FR-4.5-FR-4.7)

The original row never changes; "reversed" is derived from the reversal that points at it (ADR-0003).

```mermaid
stateDiagram-v2
    [*] --> Completed : DEPOSIT or TRANSFER committed
    Completed --> Reversed : REVERSAL committed with reverses_transaction_id
    Reversed --> Reversed : second reversal -> 409 TRANSACTION_ALREADY_REVERSED
    Completed --> Completed : reversal would make a wallet negative -> 422 INSUFFICIENT_FUNDS
    state "REVERSAL transaction" as RevTxn
    [*] --> RevTxn : created by a reversal
    RevTxn --> RevTxn : reversing it -> 422 TRANSACTION_NOT_REVERSIBLE
```

## 7. Ledger reconciliation (FR-5)

```mermaid
sequenceDiagram
    autonumber
    participant A as Administrator
    participant AC as AdminController
    participant RS as ReconciliationService
    participant DB as SQLite (WAL snapshot)
    A->>AC: GET /admin/reconciliation (ADMIN key)
    AC->>RS: reconcile()
    RS->>DB: BEGIN (read transaction - one consistent snapshot, FR-5.1)
    RS->>DB: sum DEBIT / CREDIT per currency (FR-5.2)
    RS->>DB: per transaction: balanced, >= 2 entries, one currency (FR-5.3)
    RS->>DB: per wallet: stored balance = sum CREDIT - sum DEBIT, non-system >= 0 (FR-5.4)
    RS->>DB: all entries by sequence: recompute prev_hash / entry_hash, no gaps (FR-5.5)
    RS->>DB: COMMIT (nothing written)
    RS-->>AC: report {status PASSED/FAILED, counts, totals, discrepancies} (FR-5.6)
    AC-->>A: 200 report (also when FAILED)
```

## 8. Queries and health (FR-6)

```mermaid
sequenceDiagram
    autonumber
    participant C as Caller
    participant HC as HealthController
    participant DH as DatabaseHealth
    participant DB as SQLite
    C->>HC: GET /health (no key needed)
    HC->>DH: isAvailable()
    DH->>DB: SELECT 1
    alt query succeeds
        HC-->>C: 200 {"status":"ok","database":"ok","version":"<release>"}
    else query fails
        HC-->>C: 503 {"status":"degraded","database":"unavailable","version":"<release>"}
    end
```

Other reads (`GET` user, wallet, entries, transaction) run in a read transaction, see only committed data (FR-6.5), return 400 for a malformed ID and 404 when not found; the entries list is ordered by `sequence` and paginated with `limit` (1-100, default 50) and `offset` (>= 0) plus the total count (FR-6.2).
