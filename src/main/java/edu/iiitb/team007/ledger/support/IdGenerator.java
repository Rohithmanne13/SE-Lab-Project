package edu.iiitb.team007.ledger.support;

import java.util.UUID;

/** Provider interface for new identifiers (CON-5, SE-6); tests can substitute predictable IDs. */
public interface IdGenerator {

    /** A new identifier; stored and returned as canonical lower-case UUID text (DR-5). */
    UUID newId();
}
