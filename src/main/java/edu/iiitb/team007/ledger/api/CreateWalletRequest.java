package edu.iiitb.team007.ledger.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Body of POST /users/{userId}/wallets (FR-1.3): an upper-case three-letter ISO 4217 code. */
public record CreateWalletRequest(
        @NotNull(message = "is required")
        @Pattern(regexp = "^[A-Z]{3}$", message = "must be an upper-case three-letter ISO 4217 code, e.g. INR")
        String currency) {
}
