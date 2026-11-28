package edu.iiitb.team007.ledger.service;

import java.util.List;

/**
 * The reconciliation report (FR-5.6). {@code status} is PASSED only when every check passed; a FAILED report is
 * still the requested result and is returned with HTTP 200.
 *
 * @param totalDebits  sum of all DEBIT entries over every currency (FR-5.2 "in total")
 * @param totalCredits sum of all CREDIT entries over every currency
 * @param totals       per-currency sums (FR-5.2)
 */
public record ReconciliationReport(String status, String checkedAt, long transactionsChecked, long entriesChecked,
                                   long walletsChecked, long totalDebits, long totalCredits,
                                   List<CurrencyTotals> totals, List<Discrepancy> discrepancies) {

    public static final String PASSED = "PASSED";
    public static final String FAILED = "FAILED";

    /** Sums of one currency. */
    public record CurrencyTotals(String currency, long totalDebits, long totalCredits) {
    }

    /**
     * One failed check: its name, the affected wallet, transaction or entry (the others are null) and the expected
     * and actual values. Money values are JSON integers; hashes and currencies are strings.
     */
    public record Discrepancy(String check, String currency, String walletId, String transactionId, String entryId,
                              Object expected, Object actual) {
    }
}
