package edu.iiitb.team007.ledger.support;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/** Strict parsing of identifiers: only the canonical 8-4-4-4-12 hex UUID form is accepted (FR-1.9, DR-5). */
public final class Uuids {

    private static final Pattern CANONICAL =
            Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private Uuids() {
    }

    /** The UUID, or empty if the text is not a well-formed canonical UUID. */
    public static Optional<UUID> parse(String text) {
        if (text == null || !CANONICAL.matcher(text).matches()) {
            return Optional.empty();
        }
        return Optional.of(UUID.fromString(text));
    }
}
