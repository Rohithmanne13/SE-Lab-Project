package edu.iiitb.team007.ledger.testsupport;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Test databases are fresh files on a local disk, outside the repository (TQ-5, NFR-SAFE-5, AS-1). */
class TestDatabaseTest {

    @Test
    void databasesLiveInTheSystemTemporaryFolderNotInTheRepository() {
        // Regression (Stage 3 defect log D4): they were under target/, i.e. on the Windows drive in WSL clones.
        Path temp = Path.of(System.getProperty("java.io.tmpdir")).toAbsolutePath().normalize();
        Path repo = Path.of("").toAbsolutePath().normalize();
        Path shared = TestDatabase.shared().toAbsolutePath().normalize();
        assertThat(shared.startsWith(temp)).as(shared + " is in " + temp).isTrue();
        assertThat(shared.startsWith(repo)).as(shared + " is outside " + repo).isFalse();
    }

    @Test
    void everyFreshDatabaseIsANewFile() {
        Path a = TestDatabase.fresh("x");
        Path b = TestDatabase.fresh("x");
        assertThat(a).isNotEqualTo(b).doesNotExist();
        assertThat(a.getParent()).exists();
    }
}
