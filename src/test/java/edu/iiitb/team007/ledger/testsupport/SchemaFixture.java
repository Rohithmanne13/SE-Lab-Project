package edu.iiitb.team007.ledger.testsupport;

import edu.iiitb.team007.ledger.persistence.SqlScript;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/** A raw JDBC connection to a fresh database file with the V1 schema applied, without Spring (TQ-5). */
public final class SchemaFixture {

    private SchemaFixture() {
    }

    public static Connection freshSchema() throws SQLException {
        Connection c = DriverManager.getConnection("jdbc:sqlite:" + TestDatabase.fresh("schema"));
        try (Statement s = c.createStatement()) {
            s.execute("PRAGMA foreign_keys = ON");
            for (String statement : SqlScript.statements(v1())) {
                s.execute(statement);
            }
        }
        return c;
    }

    public static String v1() {
        try (InputStream in = SchemaFixture.class.getResourceAsStream("/db/migration/V1__schema.sql")) {
            if (in == null) {
                throw new IllegalStateException("V1__schema.sql not on the classpath");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
