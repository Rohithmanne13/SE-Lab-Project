package edu.iiitb.team007.ledger.testsupport;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base class of the integration and system tests: the real service on a random port, against the shared
 * isolated test database (TQ-5). Subclasses share one Spring context, so the suite stays fast.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class IntegrationTestSupport {

    @DynamicPropertySource
    static void isolatedDatabase(DynamicPropertyRegistry registry) {
        registry.add("ledger.db-path", () -> TestDatabase.shared().toString());
    }
}
