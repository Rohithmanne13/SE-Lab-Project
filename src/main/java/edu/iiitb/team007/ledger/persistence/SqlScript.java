package edu.iiitb.team007.ledger.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Splits a migration script into statements (SI-4). A statement ends with a line ending in ";"; a
 * CREATE TRIGGER statement ends with the line ending in "END;". Lines starting with "--" are comments.
 */
public final class SqlScript {

    private SqlScript() {
    }

    public static List<String> statements(String script) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean trigger = false;
        for (String line : script.split("\\R")) {
            String trimmed = line.strip();
            if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                continue;
            }
            String upper = trimmed.toUpperCase(Locale.ROOT);
            if (current.isEmpty()) {
                trigger = upper.startsWith("CREATE TRIGGER");
            }
            current.append(line).append('\n');
            boolean ends = trigger ? upper.endsWith("END;") : trimmed.endsWith(";");
            if (ends) {
                statements.add(current.toString().strip());
                current.setLength(0);
                trigger = false;
            }
        }
        if (!current.isEmpty()) {
            throw new IllegalArgumentException("The script ends inside a statement: " + current.toString().strip());
        }
        return statements;
    }
}
