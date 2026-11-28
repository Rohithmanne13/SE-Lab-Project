package edu.iiitb.team007.ledger.persistence;

import edu.iiitb.team007.ledger.domain.ApiException;
import edu.iiitb.team007.ledger.domain.ErrorCode;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * SQLite implementation of {@link TransactionRunner} (ADR-0002). It issues the transaction statements itself -
 * {@code BEGIN IMMEDIATE} for writes, {@code BEGIN DEFERRED} for reads, then {@code COMMIT} or
 * {@code ROLLBACK} (SI-3) - and binds the connection to the current thread so the repositories use it.
 */
@Component
public class SqliteTransactionRunner implements TransactionRunner {

    private final DataSource dataSource;

    public SqliteTransactionRunner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public <T> T inWriteTransaction(Supplier<T> work) {
        return run("BEGIN IMMEDIATE", work);
    }

    @Override
    public <T> T inReadTransaction(Supplier<T> work) {
        return run("BEGIN DEFERRED", work);
    }

    private <T> T run(String begin, Supplier<T> work) {
        if (TransactionSynchronizationManager.hasResource(dataSource)) {
            throw new IllegalStateException("A transaction is already open on this thread; nesting is not allowed");
        }
        Connection connection = open();
        try {
            execute(connection, begin);
            TransactionSynchronizationManager.bindResource(dataSource, new ConnectionHolder(connection));
            try {
                T result = work.get();
                execute(connection, "COMMIT");
                return result;
            } catch (RuntimeException | Error e) {
                rollbackQuietly(connection);
                throw e;
            } finally {
                TransactionSynchronizationManager.unbindResource(dataSource);
            }
        } finally {
            closeQuietly(connection);
        }
    }

    private Connection open() {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            throw translate(e);
        }
    }

    private static void execute(Connection connection, String sql) {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            throw translate(e);
        }
    }

    private static RuntimeException translate(SQLException e) {
        if (SqliteErrors.isBusy(e)) {
            return new ApiException(ErrorCode.SERVICE_BUSY,
                    "The ledger is busy with other requests; nothing was changed - retry after the Retry-After delay.");
        }
        return new IllegalStateException("Database operation failed", e);
    }

    private static void rollbackQuietly(Connection connection) {
        try (Statement statement = connection.createStatement()) {
            statement.execute("ROLLBACK");
        } catch (SQLException ignored) {
            // the transaction was never started or SQLite already rolled it back; closing discards it anyway
        }
    }

    private static void closeQuietly(Connection connection) {
        try {
            connection.close();
        } catch (SQLException ignored) {
            // nothing more can be done; an open transaction is rolled back when the connection closes
        }
    }
}
