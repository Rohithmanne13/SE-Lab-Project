package edu.iiitb.team007.ledger.inspection;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Automated inspection for CON-3 and TQ-6: no binary floating-point or decimal type appears anywhere in the
 * service code or in the schema, so money can only be an integer of minor units.
 */
class MoneyTypesInspectionTest {

    /** The primitive types, or the boxed/decimal types used as a type (not e.g. CoercionInputShape.Float). */
    private static final Pattern JAVA_FLOATING =
            Pattern.compile("\\b(double|float)\\b|(?<![.\\w])(Double|Float|BigDecimal)\\b");
    private static final Pattern SQL_FLOATING = Pattern.compile("\\b(REAL|FLOAT|DOUBLE|NUMERIC|DECIMAL)\\b");

    @Test
    void serviceCodeUsesNoFloatingPointTypes() throws IOException {
        List<String> hits = new ArrayList<>();
        try (Stream<Path> files = Files.walk(Path.of("src", "main", "java"))) {
            for (Path f : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                List<String> lines = Files.readAllLines(f);
                for (int i = 0; i < lines.size(); i++) {
                    String code = lines.get(i).replaceAll("\"[^\"]*\"", "\"\"").replaceAll("//.*", "");
                    if (!code.strip().startsWith("*") && JAVA_FLOATING.matcher(code).find()) {
                        hits.add(f + ":" + (i + 1) + ": " + lines.get(i).strip());
                    }
                }
            }
        }
        assertThat(hits).isEmpty();
    }

    @Test
    void schemaHasOnlyIntegerMoneyColumns() throws IOException {
        try (Stream<Path> files = Files.list(Path.of("src", "main", "resources", "db", "migration"))) {
            for (Path f : files.toList()) {
                String sql = Files.readString(f).replaceAll("--.*", "");
                assertThat(SQL_FLOATING.matcher(sql).find()).as(f.toString()).isFalse();
                assertThat(sql).contains("balance     INTEGER").contains("amount                  INTEGER");
            }
        }
    }
}
