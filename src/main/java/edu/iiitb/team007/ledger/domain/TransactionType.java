package edu.iiitb.team007.ledger.domain;

/** Kinds of ledger transaction (SRS Appendix A): every one is a balanced group of entries. */
public enum TransactionType {
    DEPOSIT,
    TRANSFER,
    REVERSAL
}
