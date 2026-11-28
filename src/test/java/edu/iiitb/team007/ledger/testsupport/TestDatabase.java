package edu.iiitb.team007.ledger.testsupport;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Isolated test databases (TQ-5, NFR-SAFE-5): every test run uses fresh files in a new folder of the system
 * temporary directory - on a local disk (AS-1), never the production database file - deleted when the run ends.
 */
public final class TestDatabase {

    private static final Path RUN_DIR = createRunDir();
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
            Path dir = Files.createDirectories(RUN_DIR.resolve(UUID.randomUUID().toString()));
            return dir.resolve(name + ".db");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Path createRunDir() {
        try {
            Path dir = Files.createTempDirectory("ledger-tests-");
            Runtime.getRuntime().addShutdownHook(new Thread(() -> deleteQuietly(dir)));
            return dir;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void deleteQuietly(Path dir) {
        try (Stream<Path> files = Files.walk(dir)) {
            files.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        } catch (IOException e) {
            // best effort: the operating system clears its temporary directory anyway
        }
    }
}
