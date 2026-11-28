package edu.iiitb.team007.ledger.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import edu.iiitb.team007.ledger.testsupport.TestKeys;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Every connection carries the SI-2 settings that ADR-0002 relies on (TQ-2, TQ-5). */
@SpringBootTest
class SqliteConnectionSettingsTest {

    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    static void isolatedDatabase(DynamicPropertyRegistry registry) {
        registry.add("ledger.db-path", () -> tempDir.resolve("settings.db").toString());
        TestKeys.register(registry);
    }

    @Autowired
    private DataSource dataSource;

    @Test
    void connectionUsesForeignKeysWalFullSyncAndBusyTimeout() throws Exception {
        try (Connection c = dataSource.getConnection(); Statement s = c.createStatement()) {
            assertThat(pragma(s, "foreign_keys")).isEqualTo("1");
            assertThat(pragma(s, "journal_mode")).isEqualToIgnoringCase("wal");
            assertThat(pragma(s, "synchronous")).isEqualTo("2");
            assertThat(Integer.parseInt(pragma(s, "busy_timeout"))).isGreaterThanOrEqualTo(5000);
        }
    }

    @Test
    void databaseFileIsCreatedAtTheConfiguredPath() throws Exception {
        try (Connection c = dataSource.getConnection(); Statement s = c.createStatement()) {
            s.execute("SELECT 1");
        }
        assertThat(tempDir.resolve("settings.db")).exists();
    }

    private static String pragma(Statement s, String name) throws Exception {
        try (ResultSet rs = s.executeQuery("PRAGMA " + name)) {
            return rs.next() ? rs.getString(1) : null;
        }
    }
}
