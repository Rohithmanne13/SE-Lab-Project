package edu.iiitb.team007.ledger.api;

import edu.iiitb.team007.ledger.domain.Money;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Body of POST /wallets/{walletId}/deposits (FR-1.6): a positive integer amount in minor units, the wallet's
 * currency and an optional description of at most 255 characters.
 */
public record DepositRequest(
        @NotNull(message = "is required")
        @Positive(message = "must be a positive integer of minor units")
        @Max(value = Money.MAX_AMOUNT, message = "must be at most 9007199254740991")
        Long amount,
        @NotNull(message = "is required")
        @Pattern(regexp = "^[A-Z]{3}$", message = "must be an upper-case three-letter ISO 4217 code, e.g. INR")
        String currency,
        @Size(max = 255, message = "must be at most 255 characters")
        String description) {
}
