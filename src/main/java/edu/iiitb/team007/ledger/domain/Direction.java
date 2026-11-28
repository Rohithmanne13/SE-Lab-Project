package edu.iiitb.team007.ledger.domain;

/** Direction of a ledger entry (BR-8): CREDIT increases a wallet's balance, DEBIT decreases it. */
public enum Direction {
    DEBIT,
    CREDIT;

    /** The opposite direction, used for the entries of a reversal (FR-4.5). */
    public Direction opposite() {
        return this == DEBIT ? CREDIT : DEBIT;
    }
}
