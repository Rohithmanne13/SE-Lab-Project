package edu.iiitb.team007.ledger.persistence;

import edu.iiitb.team007.ledger.config.LedgerProperties;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

/**
 * The one place where SQLite connections are configured (SI-2, ADR-0002): foreign keys on, WAL journal,
 * synchronous FULL, 5000 ms busy timeout, and IMMEDIATE transactions so every transaction starts with
 * BEGIN IMMEDIATE (SI-3, FR-3.1).
 */
@Configuration
public class SqliteDataSourceConfig {

    /** Busy timeout in milliseconds; at least 5000 per SI-2 and FR-3.6. */
    public static final int BUSY_TIMEOUT_MS = 5000;

    /** Open connections kept for reuse; each one carries the settings above. */
    public static final int POOL_SIZE = 20;

    @Bean
    public DataSource dataSource(LedgerProperties properties) {
        Path file = Path.of(properties.dbPath()).toAbsolutePath();
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create the database folder " + file.getParent(), e);
        }
        SQLiteConfig config = new SQLiteConfig();
        config.enforceForeignKeys(true);
        config.setJournalMode(SQLiteConfig.JournalMode.WAL);
        config.setSynchronous(SQLiteConfig.SynchronousMode.FULL);
        config.setBusyTimeout(BUSY_TIMEOUT_MS);
        config.setTransactionMode(SQLiteConfig.TransactionMode.IMMEDIATE);
        SQLiteDataSource sqlite = new SQLiteDataSource(config);
        sqlite.setUrl("jdbc:sqlite:" + file);
        HikariConfig pool = new HikariConfig();
        pool.setDataSource(sqlite);
        pool.setPoolName("ledger-sqlite");
        pool.setMaximumPoolSize(POOL_SIZE);
        return new HikariDataSource(pool);
    }
}
