package edu.iiitb.team007.ledger.domain;

/**
 * Money rules (CON-3, NFR-REL-3, NFR-REL-6, BR-8): amounts and balances are {@code long} integers of minor
 * units - never floating point - and never exceed 2^53 - 1 in absolute value.
 */
public final class Money {

    /** 2^53 - 1 = 9 007 199 254 740 991, the largest integer exact in every JSON client (NFR-REL-6). */
    public static final long MAX_AMOUNT = 9_007_199_254_740_991L;

    private Money() {
    }

    /** The balance after one entry: CREDIT adds, DEBIT subtracts; exact integer arithmetic (NFR-REL-3). */
    public static long apply(long balance, Direction direction, long amount) {
        return direction == Direction.CREDIT ? Math.addExact(balance, amount) : Math.subtractExact(balance, amount);
    }

    /** True if the value is within -(2^53 - 1) .. 2^53 - 1 (NFR-REL-6). */
    public static boolean withinLimit(long value) {
        return value >= -MAX_AMOUNT && value <= MAX_AMOUNT;
    }

    /** True for a valid amount: 1 .. 2^53 - 1 (FR-2.1, BR-8). */
    public static boolean isValidAmount(long amount) {
        return amount >= 1 && amount <= MAX_AMOUNT;
    }
}
