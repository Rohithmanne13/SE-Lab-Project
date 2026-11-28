package edu.iiitb.team007.ledger.domain;

import java.util.Locale;

/**
 * Every error code of SRS section 3.3.4 with its HTTP status and fixed title (ADR-0004, FR-0.3, UI-3).
 * The problem {@code type} of each code links to its heading in docs/design/error-codes.md.
 */
public enum ErrorCode {
    VALIDATION_ERROR(400, "Invalid request"),
    INVALID_ID(400, "Invalid identifier"),
    IDEMPOTENCY_KEY_MISSING(400, "Idempotency-Key is missing"),
    IDEMPOTENCY_KEY_INVALID(400, "Idempotency-Key is malformed"),
    UNAUTHENTICATED(401, "Authentication required"),
    FORBIDDEN(403, "Not allowed"),
    USER_NOT_FOUND(404, "User not found"),
    WALLET_NOT_FOUND(404, "Wallet not found"),
    TRANSACTION_NOT_FOUND(404, "Transaction not found"),
    EMAIL_ALREADY_EXISTS(409, "Email already registered"),
    WALLET_ALREADY_EXISTS(409, "Wallet already exists"),
    TRANSACTION_ALREADY_REVERSED(409, "Transaction already reversed"),
    PAYLOAD_TOO_LARGE(413, "Request body too large"),
    UNSUPPORTED_MEDIA_TYPE(415, "Unsupported media type"),
    INSUFFICIENT_FUNDS(422, "Insufficient funds"),
    CURRENCY_MISMATCH(422, "Currency mismatch"),
    SAME_WALLET_TRANSFER(422, "Same source and destination"),
    UNSUPPORTED_CURRENCY(422, "Unsupported currency"),
    TRANSACTION_NOT_REVERSIBLE(422, "Transaction cannot be reversed"),
    IDEMPOTENCY_KEY_REUSED(422, "Idempotency-Key already used"),
    AMOUNT_LIMIT_EXCEEDED(422, "Amount limit exceeded"),
    INTERNAL_ERROR(500, "Internal error"),
    SERVICE_BUSY(503, "Service busy");

    /** Base of every problem type URI; the anchor is the lower-case code (ADR-0004). */
    public static final String TYPE_BASE =
            "https://github.com/Rohithmanne13/SE-Lab-Project/blob/main/docs/design/error-codes.md#";

    private final int status;
    private final String title;

    ErrorCode(int status, String title) {
        this.status = status;
        this.title = title;
    }

    public int status() {
        return status;
    }

    public String title() {
        return title;
    }

    /** Absolute, resolvable problem type URI, e.g. ...error-codes.md#insufficient_funds (RFC 9457). */
    public String typeUri() {
        return TYPE_BASE + name().toLowerCase(Locale.ROOT);
    }
}
