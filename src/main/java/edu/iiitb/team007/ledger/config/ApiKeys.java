package edu.iiitb.team007.ledger.config;

import edu.iiitb.team007.ledger.support.Sha256;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * The configured API keys (FR-0.1, SEC-5): read only from LEDGER_CLIENT_KEYS and LEDGER_ADMIN_KEYS, each at least
 * 32 characters, compared in constant time. Only SHA-256 digests are kept in memory; keys are never logged or
 * returned. A missing or too short key stops the service at start-up with a message that names no key.
 */
@Component
public class ApiKeys {

    /** Minimum key length (SEC-5). */
    public static final int MIN_LENGTH = 32;

    private record Entry(byte[] digest, ApiRole role) {
    }

    private final List<Entry> entries = new ArrayList<>();

    public ApiKeys(LedgerProperties properties) {
        add(properties.clientKeys(), ApiRole.CLIENT, "LEDGER_CLIENT_KEYS");
        add(properties.adminKeys(), ApiRole.ADMIN, "LEDGER_ADMIN_KEYS");
        Set<String> seen = new HashSet<>();
        for (String key : properties.clientKeys()) {
            seen.add(key.strip());
        }
        for (String key : properties.adminKeys()) {
            if (seen.contains(key.strip())) {
                throw new IllegalStateException("The same key is configured as CLIENT and ADMIN key");
            }
        }
    }

    private void add(List<String> keys, ApiRole role, String variable) {
        if (keys.isEmpty()) {
            throw new IllegalStateException(variable + " is not set: configure at least one key of "
                    + MIN_LENGTH + " or more random characters (see README.md)");
        }
        for (String raw : keys) {
            String key = raw.strip();
            if (key.length() < MIN_LENGTH) {
                throw new IllegalStateException(variable + " contains a key shorter than " + MIN_LENGTH
                        + " characters (SEC-5)");
            }
            entries.add(new Entry(Sha256.digest(key.getBytes(StandardCharsets.UTF_8)), role));
        }
    }

    /**
     * The role of a presented key, or empty if it matches no configured key. Every configured key is compared,
     * with {@link MessageDigest#isEqual}, on fixed-length digests, so timing does not reveal which part matched.
     */
    public Optional<ApiRole> roleOf(String presented) {
        if (presented == null || presented.isEmpty()) {
            return Optional.empty();
        }
        byte[] digest = Sha256.digest(presented.getBytes(StandardCharsets.UTF_8));
        ApiRole found = null;
        for (Entry e : entries) {
            boolean match = MessageDigest.isEqual(digest, e.digest());
            if (match && found == null) {
                found = e.role();
            }
        }
        return Optional.ofNullable(found);
    }
}
