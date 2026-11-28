package edu.iiitb.team007.ledger.persistence;

import java.sql.SQLException;

/** Recognises SQLite result codes in exceptions from the driver or from Spring JDBC. */
public final class SqliteErrors {

    private static final int SQLITE_BUSY = 5;
    private static final int SQLITE_CONSTRAINT = 19;

    private SqliteErrors() {
    }

    /** True if the exception or one of its causes is SQLITE_BUSY (database locked, FR-3.6). */
    public static boolean isBusy(Throwable t) {
        return hasPrimaryCode(t, SQLITE_BUSY);
    }

    /** True if the exception or one of its causes is a constraint violation (UNIQUE, CHECK, ...). */
    public static boolean isConstraintViolation(Throwable t) {
        return hasPrimaryCode(t, SQLITE_CONSTRAINT);
    }

    private static boolean hasPrimaryCode(Throwable t, int code) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (c instanceof SQLException e && (e.getErrorCode() & 0xFF) == code) {
                return true;
            }
            if (c.getCause() == c) {
                break;
            }
        }
        return false;
    }
}
