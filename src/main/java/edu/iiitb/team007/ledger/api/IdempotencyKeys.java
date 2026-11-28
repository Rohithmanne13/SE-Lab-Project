package edu.iiitb.team007.ledger.api;

import edu.iiitb.team007.ledger.domain.ApiException;
import edu.iiitb.team007.ledger.domain.ErrorCode;
import java.util.regex.Pattern;

/**
 * The Idempotency-Key header of deposits, transfers and reversals (FR-2.3, SRS 3.3.3): required, 8-128 characters
 * of A-Z a-z 0-9 - _. Checked before the body and before any database work.
 */
public final class IdempotencyKeys {

    /** Header name (SRS 3.3.3). */
    public static final String HEADER = "Idempotency-Key";

    private static final Pattern FORMAT = Pattern.compile("^[A-Za-z0-9_-]{8,128}$");

    private IdempotencyKeys() {
    }

    public static String require(String value) {
        if (value == null) {
            throw new ApiException(ErrorCode.IDEMPOTENCY_KEY_MISSING,
                    "Send an Idempotency-Key header (a new UUID v4 is recommended) so a retry never charges twice.");
        }
        if (!FORMAT.matcher(value).matches()) {
            throw new ApiException(ErrorCode.IDEMPOTENCY_KEY_INVALID,
                    "The Idempotency-Key must be 8-128 characters of A-Z, a-z, 0-9, '-' and '_'.");
        }
        return value;
    }
}
