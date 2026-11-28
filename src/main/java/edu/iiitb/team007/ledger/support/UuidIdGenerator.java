package edu.iiitb.team007.ledger.support;

import java.util.UUID;
import org.springframework.stereotype.Component;

/** Production identifiers: random (version 4) UUIDs, RFC 9562. */
@Component
public class UuidIdGenerator implements IdGenerator {

    @Override
    public UUID newId() {
        return UUID.randomUUID();
    }
}
