package edu.iiitb.team007.ledger.domain;

import java.util.UUID;

/**
 * A user who owns wallets (DR-1, FR-1.1). System users own the system funding accounts (FR-1.5).
 *
 * @param createdAt RFC 3339 UTC timestamp with milliseconds, as stored (DR-5)
 */
public record User(UUID userId, String name, String email, boolean system, String createdAt) {
}
