package edu.iiitb.team007.ledger.domain;

import java.util.Collection;

/**
 * Accounting calculations shared by posting and reconciliation (TQ-1): totals per direction, the balance
 * of a set of entries and the balanced-transaction rule (FR-1.8, BR-1).
 */
public final class LedgerMath {

    private LedgerMath() {
    }

    /** Sum of the amounts of the entries in one direction. */
    public static long total(Collection<LedgerEntry> entries, Direction direction) {
        long sum = 0;
        for (LedgerEntry e : entries) {
            if (e.direction() == direction) {
                sum = Math.addExact(sum, e.amount());
            }
        }
        return sum;
    }

    /** Balance represented by the entries: sum of CREDIT minus sum of DEBIT (FR-1.7, FR-5.4). */
    public static long balance(Collection<LedgerEntry> entries) {
        return Math.subtractExact(total(entries, Direction.CREDIT), total(entries, Direction.DEBIT));
    }

    /**
     * FR-1.8: a transaction is valid only with at least two entries, all in one currency, every amount
     * positive, and total DEBIT equal to total CREDIT.
     */
    public static boolean isBalanced(Collection<LedgerEntry> entries) {
        if (entries.size() < 2) {
            return false;
        }
        String currency = entries.iterator().next().currency();
        for (LedgerEntry e : entries) {
            if (!e.currency().equals(currency) || e.amount() <= 0) {
                return false;
            }
        }
        return total(entries, Direction.DEBIT) == total(entries, Direction.CREDIT);
    }
}
