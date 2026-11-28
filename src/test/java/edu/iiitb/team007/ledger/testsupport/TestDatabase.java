package edu.iiitb.team007.ledger.testsupport;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Isolated test databases (TQ-5, NFR-SAFE-5): every test run uses fresh files under target/test-db/, never the
 * production database file.
 */
public final class TestDatabase {

    private static final Path SHARED = fresh("shared");

    private TestDatabase() {
    }

    /** The database shared by the integration tests of one test run (one Spring context). */
    public static Path shared() {
        return SHARED;
    }

    /** A new, empty database file path for tests that need their own database. */
    public static Path fresh(String name) {
        try {
            Path dir = Path.of("target", "test-db", UUID.randomUUID().toString()).toAbsolutePath();
            Files.createDirectories(dir);
            return dir.resolve(name + ".db");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
