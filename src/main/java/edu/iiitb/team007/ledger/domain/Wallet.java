package edu.iiitb.team007.ledger.domain;

import java.util.UUID;

/**
 * A wallet holding money of one currency for one user (DR-1, FR-1.3).
 *
 * @param balance stored balance in integer minor units (CON-3); always equals the sum of CREDIT minus the
 *                sum of DEBIT entries of the wallet (FR-1.7)
 * @param system  true for a system funding account, the only kind allowed below zero (FR-1.5, BR-3)
 */
public record Wallet(UUID walletId, UUID userId, String currency, long balance, boolean system, String createdAt) {
}
