package edu.iiitb.team007.ledger.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed configuration (OE-3, DR-6). Values come from environment variables via application.properties.
 *
 * @param dbPath  SQLite database file; tests point this at a fresh temporary file (TQ-5)
 * @param version release version reported by GET /health (FR-6.4)
 */
@ConfigurationProperties(prefix = "ledger")
public record LedgerProperties(String dbPath, String version) {
}
