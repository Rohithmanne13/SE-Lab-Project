package edu.iiitb.team007.ledger.persistence;

import java.util.function.Supplier;

/**
 * Units of work on the database (ADR-0002, CON-5). Every repository call happens inside one of them.
 */
public interface TransactionRunner {

    /**
     * Runs the work in one write transaction started with BEGIN IMMEDIATE, so the write lock is held from the
     * first read to COMMIT (SI-3, FR-3.1). Any exception rolls everything back. If the lock is not obtained
     * within the busy timeout the call fails with SERVICE_BUSY and nothing is written (FR-3.6).
     */
    <T> T inWriteTransaction(Supplier<T> work);

    /** Runs the work in one read transaction: one consistent snapshot that never blocks the writer (FR-5.1). */
    <T> T inReadTransaction(Supplier<T> work);
}
