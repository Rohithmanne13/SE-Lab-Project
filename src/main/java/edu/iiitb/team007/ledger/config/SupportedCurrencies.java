package edu.iiitb.team007.ledger.config;

import java.util.Currency;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * The configured set of supported currencies (FR-1.3): upper-case three-letter ISO 4217 codes, always
 * including INR and USD. Checked once at start-up; a bad setting stops the service with a clear message.
 */
@Component
public class SupportedCurrencies {

    /** Format of a currency code in requests; a well-formed but unsupported code is 422 (FR-1.9). */
    public static final Pattern FORMAT = Pattern.compile("^[A-Z]{3}$");

    private static final List<String> REQUIRED = List.of("INR", "USD");

    private final Set<String> codes;

    public SupportedCurrencies(LedgerProperties properties) {
        Set<String> configured = new LinkedHashSet<>();
        for (String raw : properties.currencies()) {
            String code = raw.strip();
            if (!FORMAT.matcher(code).matches() || !isIso4217(code)) {
                throw new IllegalStateException("LEDGER_CURRENCIES contains '" + code
                        + "', which is not an upper-case ISO 4217 currency code");
            }
            configured.add(code);
        }
        if (!configured.containsAll(REQUIRED)) {
            throw new IllegalStateException("LEDGER_CURRENCIES must include at least INR and USD (FR-1.3)");
        }
        this.codes = Set.copyOf(configured);
    }

    public boolean isSupported(String code) {
        return codes.contains(code);
    }

    /** All supported codes in a stable (sorted) order. */
    public List<String> all() {
        return codes.stream().sorted().toList();
    }

    private static boolean isIso4217(String code) {
        try {
            return Currency.getInstance(code).getCurrencyCode().equals(code.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
