package edu.iiitb.team007.ledger.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed configuration (OE-3, DR-6, FR-1.3, SEC-5). Values come from environment variables via
 * application.properties.
 *
 * @param dbPath     SQLite database file; tests point this at a fresh file (TQ-5)
 * @param version    release version reported by GET /health (FR-6.4)
 * @param currencies supported ISO 4217 currency codes, at least INR and USD (FR-1.3)
 * @param clientKeys CLIENT API keys from LEDGER_CLIENT_KEYS (FR-0.1)
 * @param adminKeys  ADMIN API keys from LEDGER_ADMIN_KEYS (FR-0.1)
 */
@ConfigurationProperties(prefix = "ledger")
public record LedgerProperties(String dbPath, String version, List<String> currencies, List<String> clientKeys,
                               List<String> adminKeys) {

    public LedgerProperties {
        currencies = currencies == null ? List.of() : List.copyOf(currencies);
        clientKeys = clientKeys == null ? List.of() : List.copyOf(clientKeys);
        adminKeys = adminKeys == null ? List.of() : List.copyOf(adminKeys);
    }

    /** Never print the keys (SEC-5, SEC-7). */
    @Override
    public String toString() {
        return "LedgerProperties[dbPath=" + dbPath + ", version=" + version + ", currencies=" + currencies
                + ", clientKeys=" + clientKeys.size() + " key(s), adminKeys=" + adminKeys.size() + " key(s)]";
    }
}
