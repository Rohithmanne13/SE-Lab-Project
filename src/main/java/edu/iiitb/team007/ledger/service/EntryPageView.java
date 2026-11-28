package edu.iiitb.team007.ledger.service;

import java.util.List;
import java.util.UUID;

/** JSON of GET /wallets/{walletId}/entries (FR-6.2): one page in ascending sequence plus the total count. */
public record EntryPageView(UUID walletId, List<EntryView> entries, long totalCount, int limit, int offset) {
}
