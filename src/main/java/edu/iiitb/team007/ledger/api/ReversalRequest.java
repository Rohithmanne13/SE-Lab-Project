package edu.iiitb.team007.ledger.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Body of POST /transactions/{transactionId}/reversal (FR-4.5): a reason of 1-255 characters. */
public record ReversalRequest(
        @NotNull(message = "is required")
        @Size(min = 1, max = 255, message = "must be 1 to 255 characters")
        String reason) {
}
