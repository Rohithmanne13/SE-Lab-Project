package edu.iiitb.team007.ledger.persistence;

/** Provider interface for checking that the database is usable (SE-6, CON-5); tests substitute it. */
public interface DatabaseHealth {

    /** Returns true when a trivial query on the database succeeds (FR-6.4). */
    boolean isAvailable();
}
