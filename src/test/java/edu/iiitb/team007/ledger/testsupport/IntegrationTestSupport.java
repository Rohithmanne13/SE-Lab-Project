package edu.iiitb.team007.ledger.testsupport;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base class of the integration and system tests that share one isolated test database (TQ-5) and the random
 * test API keys. Subclasses share one Spring context, so the suite stays fast. A test that must not touch the
 * shared database extends {@link ApiTestSupport} directly and registers its own database.
 */
public abstract class IntegrationTestSupport extends ApiTestSupport {

    @DynamicPropertySource
    static void sharedDatabaseAndKeys(DynamicPropertyRegistry registry) {
        registry.add("ledger.db-path", () -> TestDatabase.shared().toString());
        TestKeys.register(registry);
    }
}
