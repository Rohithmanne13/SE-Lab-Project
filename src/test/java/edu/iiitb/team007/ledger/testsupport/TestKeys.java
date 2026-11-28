package edu.iiitb.team007.ledger.testsupport;

import java.security.SecureRandom;
import java.util.HexFormat;
import org.springframework.test.context.DynamicPropertyRegistry;

/**
 * API keys for tests, generated at random for every test run - no key is ever written in the repository
 * (CON-9, SEC-5).
 */
public final class TestKeys {

    public static final String CLIENT = random();
    public static final String ADMIN = random();

    private TestKeys() {
    }

    /** 48 random hex characters (at least 32 are required, SEC-5). */
    public static String random() {
        byte[] bytes = new byte[24];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    public static void register(DynamicPropertyRegistry registry) {
        registry.add("ledger.client-keys", () -> CLIENT);
        registry.add("ledger.admin-keys", () -> ADMIN);
    }
}
