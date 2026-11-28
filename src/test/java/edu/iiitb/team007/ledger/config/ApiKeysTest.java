package edu.iiitb.team007.ledger.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import edu.iiitb.team007.ledger.testsupport.TestKeys;
import java.util.List;
import org.junit.jupiter.api.Test;

/** API keys from the environment: at least 32 characters, roles, constant-time matching (FR-0.1, SEC-5). */
class ApiKeysTest {

    private static final String CLIENT = TestKeys.random();
    private static final String ADMIN = TestKeys.random();

    private static ApiKeys keys(List<String> client, List<String> admin) {
        return new ApiKeys(new LedgerProperties("x.db", "1", List.of("INR", "USD"), client, admin));
    }

    @Test
    void keysMapToTheirRoles() {
        ApiKeys k = keys(List.of(CLIENT), List.of(ADMIN));
        assertThat(k.roleOf(CLIENT)).contains(ApiRole.CLIENT);
        assertThat(k.roleOf(ADMIN)).contains(ApiRole.ADMIN);
        assertThat(k.roleOf(CLIENT + "x")).isEmpty();
        assertThat(k.roleOf("")).isEmpty();
        assertThat(k.roleOf(null)).isEmpty();
    }

    @Test
    void severalKeysPerRoleAreAllowed() {
        String second = TestKeys.random();
        ApiKeys k = keys(List.of(CLIENT, second), List.of(ADMIN));
        assertThat(k.roleOf(second)).contains(ApiRole.CLIENT);
    }

    @Test
    void missingShortOrSharedKeysStopTheServiceWithoutPrintingThem() {
        assertThatThrownBy(() -> keys(List.of(), List.of(ADMIN))).hasMessageContaining("LEDGER_CLIENT_KEYS is not set");
        assertThatThrownBy(() -> keys(List.of(CLIENT), List.of())).hasMessageContaining("LEDGER_ADMIN_KEYS is not set");
        assertThatThrownBy(() -> keys(List.of("short-key"), List.of(ADMIN)))
                .hasMessageContaining("shorter than 32").hasMessageNotContaining("short-key");
        assertThatThrownBy(() -> keys(List.of(CLIENT), List.of(CLIENT)))
                .hasMessageContaining("CLIENT and ADMIN").hasMessageNotContaining(CLIENT);
    }

    @Test
    void propertiesNeverPrintKeys() {
        LedgerProperties p = new LedgerProperties("x.db", "1", List.of("INR"), List.of(CLIENT), List.of(ADMIN));
        assertThat(p.toString()).doesNotContain(CLIENT).doesNotContain(ADMIN).contains("1 key(s)");
    }
}
