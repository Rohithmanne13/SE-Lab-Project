package edu.iiitb.team007.ledger.persistence;

import edu.iiitb.team007.ledger.support.Timestamps;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

/**
 * Creates and evolves the schema at start-up from the versioned scripts
 * {@code classpath:db/migration/V<n>__<description>.sql} (SI-4). Pending versions are applied in order, in one
 * write transaction, and recorded in {@code schema_version}; versions already applied are skipped.
 */
@Component
public class MigrationRunner implements InitializingBean {

    private static final String LOCATION = "classpath:db/migration/V*__*.sql";
    private static final Pattern FILE_NAME = Pattern.compile("^V(\\d+)__(.+)\\.sql$");

    private final TransactionRunner transactions;
    private final LedgerJdbc db;
    private final Clock clock;

    /** One migration script. */
    public record Migration(int version, String description, String sql) {
    }

    public MigrationRunner(TransactionRunner transactions, LedgerJdbc db, Clock clock) {
        this.transactions = transactions;
        this.db = db;
        this.clock = clock;
    }

    @Override
    public void afterPropertiesSet() {
        migrate();
    }

    /** Applies every pending migration; returns how many were applied. */
    public int migrate() {
        List<Migration> migrations = load();
        return transactions.inWriteTransaction(() -> {
            int current = currentVersion();
            int applied = 0;
            for (Migration m : migrations) {
                if (m.version() <= current) {
                    continue;
                }
                for (String statement : SqlScript.statements(m.sql())) {
                    db.template().execute(statement);
                }
                db.template().update("INSERT INTO schema_version (version, description, applied_at) VALUES (?, ?, ?)",
                        m.version(), m.description(), Timestamps.now(clock));
                applied++;
            }
            return applied;
        });
    }

    /** The highest applied version, 0 for an empty database. */
    public int currentVersion() {
        Integer tables = db.template().queryForObject(
                "SELECT count(*) FROM sqlite_master WHERE type = 'table' AND name = 'schema_version'", Integer.class);
        if (tables == null || tables == 0) {
            return 0;
        }
        Integer version = db.template().queryForObject("SELECT COALESCE(MAX(version), 0) FROM schema_version",
                Integer.class);
        return version == null ? 0 : version;
    }

    static List<Migration> load() {
        try {
            List<Migration> migrations = new ArrayList<>();
            for (Resource r : new PathMatchingResourcePatternResolver().getResources(LOCATION)) {
                Matcher m = FILE_NAME.matcher(r.getFilename() == null ? "" : r.getFilename());
                if (!m.matches()) {
                    throw new IllegalStateException("Badly named migration script " + r.getFilename());
                }
                migrations.add(new Migration(Integer.parseInt(m.group(1)), m.group(2).replace('_', ' '),
                        r.getContentAsString(StandardCharsets.UTF_8)));
            }
            migrations.sort(Comparator.comparingInt(Migration::version));
            for (int i = 1; i < migrations.size(); i++) {
                if (migrations.get(i).version() == migrations.get(i - 1).version()) {
                    throw new IllegalStateException("Two migration scripts share version "
                            + migrations.get(i).version());
                }
            }
            return migrations;
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read the migration scripts", e);
        }
    }
}
