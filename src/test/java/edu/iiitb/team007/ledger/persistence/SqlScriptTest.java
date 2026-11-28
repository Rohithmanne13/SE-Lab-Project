package edu.iiitb.team007.ledger.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import edu.iiitb.team007.ledger.testsupport.SchemaFixture;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Splitting migration scripts into statements, including trigger bodies (SI-4). */
class SqlScriptTest {

    @Test
    void splitsStatementsAndKeepsTriggerBodiesWhole() {
        List<String> s = SqlScript.statements("-- comment\nCREATE TABLE a (x INTEGER);\n\n"
                + "CREATE TRIGGER t BEFORE DELETE ON a\nBEGIN SELECT RAISE(ABORT, 'no'); END;\n"
                + "CREATE INDEX i ON a (x);\n");
        assertThat(s).containsExactly("CREATE TABLE a (x INTEGER);",
                "CREATE TRIGGER t BEFORE DELETE ON a\nBEGIN SELECT RAISE(ABORT, 'no'); END;",
                "CREATE INDEX i ON a (x);");
    }

    @Test
    void rejectsAnUnterminatedStatement() {
        assertThatThrownBy(() -> SqlScript.statements("CREATE TABLE a (x INTEGER)"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void v1SchemaHasSixTablesTwoIndexesAndFourTriggers() {
        List<String> s = SqlScript.statements(SchemaFixture.v1());
        assertThat(s).filteredOn(x -> x.startsWith("CREATE TABLE")).hasSize(6);
        assertThat(s).filteredOn(x -> x.startsWith("CREATE INDEX")).hasSize(2);
        assertThat(s).filteredOn(x -> x.startsWith("CREATE TRIGGER")).hasSize(4);
    }
}
