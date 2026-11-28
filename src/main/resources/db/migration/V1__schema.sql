-- V1__schema.sql - Financial Ledger & Digital Wallet API (DR-1..DR-7, FR-4.2)

CREATE TABLE users (
    user_id     TEXT    PRIMARY KEY CHECK (length(user_id) = 36),
    name        TEXT    NOT NULL CHECK (length(name) BETWEEN 1 AND 100),
    email       TEXT    NOT NULL COLLATE NOCASE UNIQUE CHECK (length(email) <= 254),
    is_system   INTEGER NOT NULL DEFAULT 0 CHECK (is_system IN (0, 1)),
    created_at  TEXT    NOT NULL
);

CREATE TABLE wallets (
    wallet_id   TEXT    PRIMARY KEY CHECK (length(wallet_id) = 36),
    user_id     TEXT    NOT NULL REFERENCES users (user_id),
    currency    TEXT    NOT NULL CHECK (length(currency) = 3 AND currency = upper(currency)),
    balance     INTEGER NOT NULL DEFAULT 0
                CHECK (balance BETWEEN -9007199254740991 AND 9007199254740991),
    is_system   INTEGER NOT NULL DEFAULT 0 CHECK (is_system IN (0, 1)),
    created_at  TEXT    NOT NULL,
    UNIQUE (user_id, currency),
    CHECK (is_system = 1 OR balance >= 0)
);

CREATE TABLE transactions (
    transaction_id          TEXT    PRIMARY KEY CHECK (length(transaction_id) = 36),
    type                    TEXT    NOT NULL CHECK (type IN ('DEPOSIT', 'TRANSFER', 'REVERSAL')),
    status                  TEXT    NOT NULL DEFAULT 'COMPLETED' CHECK (status = 'COMPLETED'),
    currency                TEXT    NOT NULL CHECK (length(currency) = 3),
    amount                  INTEGER NOT NULL CHECK (amount > 0 AND amount <= 9007199254740991),
    description             TEXT    CHECK (description IS NULL OR length(description) <= 255),
    reverses_transaction_id TEXT    UNIQUE REFERENCES transactions (transaction_id),
    created_at              TEXT    NOT NULL,
    CHECK ((type = 'REVERSAL') = (reverses_transaction_id IS NOT NULL))
);

CREATE TABLE ledger_entries (
    entry_id        TEXT    PRIMARY KEY CHECK (length(entry_id) = 36),
    sequence        INTEGER NOT NULL UNIQUE CHECK (sequence >= 1),
    transaction_id  TEXT    NOT NULL REFERENCES transactions (transaction_id),
    wallet_id       TEXT    NOT NULL REFERENCES wallets (wallet_id),
    direction       TEXT    NOT NULL CHECK (direction IN ('DEBIT', 'CREDIT')),
    amount          INTEGER NOT NULL CHECK (amount > 0 AND amount <= 9007199254740991),
    currency        TEXT    NOT NULL CHECK (length(currency) = 3),
    created_at      TEXT    NOT NULL,
    prev_hash       TEXT    NOT NULL CHECK (length(prev_hash) = 64),
    entry_hash      TEXT    NOT NULL UNIQUE CHECK (length(entry_hash) = 64)
);

CREATE INDEX ix_ledger_entries_wallet      ON ledger_entries (wallet_id, sequence);
CREATE INDEX ix_ledger_entries_transaction ON ledger_entries (transaction_id);

CREATE TABLE idempotency_records (
    idempotency_key TEXT    PRIMARY KEY CHECK (length(idempotency_key) BETWEEN 8 AND 128),
    request_hash    TEXT    NOT NULL CHECK (length(request_hash) = 64),
    transaction_id  TEXT    NOT NULL REFERENCES transactions (transaction_id),
    response_status INTEGER NOT NULL,
    response_body   TEXT    NOT NULL,
    created_at      TEXT    NOT NULL
);

-- Append-only ledger (FR-4.2, DR-4, BR-4)
CREATE TRIGGER trg_transactions_no_update BEFORE UPDATE ON transactions
BEGIN SELECT RAISE(ABORT, 'transactions is append-only'); END;
CREATE TRIGGER trg_transactions_no_delete BEFORE DELETE ON transactions
BEGIN SELECT RAISE(ABORT, 'transactions is append-only'); END;
CREATE TRIGGER trg_ledger_entries_no_update BEFORE UPDATE ON ledger_entries
BEGIN SELECT RAISE(ABORT, 'ledger_entries is append-only'); END;
CREATE TRIGGER trg_ledger_entries_no_delete BEFORE DELETE ON ledger_entries
BEGIN SELECT RAISE(ABORT, 'ledger_entries is append-only'); END;

-- Applied migrations (SI-4)
CREATE TABLE schema_version (
    version     INTEGER PRIMARY KEY,
    description TEXT    NOT NULL,
    applied_at  TEXT    NOT NULL
);
