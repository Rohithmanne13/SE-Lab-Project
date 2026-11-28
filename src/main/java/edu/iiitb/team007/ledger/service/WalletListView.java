package edu.iiitb.team007.ledger.service;

import java.util.List;
import java.util.UUID;

/** JSON of GET /users/{userId}/wallets (FR-6.1). */
public record WalletListView(UUID userId, List<WalletView> wallets) {
}
