package edu.iiitb.team007.ledger.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed configuration (OE-3, DR-6, FR-1.3). Values come from environment variables via
 * application.properties.
 *
 * @param dbPath     SQLite database file; tests point this at a fresh file (TQ-5)
 * @param version    release version reported by GET /health (FR-6.4)
 * @param currencies supported ISO 4217 currency codes, at least INR and USD (FR-1.3)
 */
@ConfigurationProperties(prefix = "ledger")
public record LedgerProperties(String dbPath, String version, List<String> currencies) {

    public LedgerProperties {
        currencies = currencies == null ? List.of() : List.copyOf(currencies);
    }
}
