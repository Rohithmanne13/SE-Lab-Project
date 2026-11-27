package edu.iiitb.team007.ledger.api;

import edu.iiitb.team007.ledger.config.LedgerProperties;
import edu.iiitb.team007.ledger.persistence.DatabaseHealth;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /health - liveness and database connectivity (FR-6.4). Served at the root, not under /api/v1;
 * needs no API key and performs no write.
 */
@RestController
public class HealthController {

    private final DatabaseHealth databaseHealth;
    private final LedgerProperties properties;

    public HealthController(DatabaseHealth databaseHealth, LedgerProperties properties) {
        this.databaseHealth = databaseHealth;
        this.properties = properties;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        boolean up = databaseHealth.isAvailable();
        Map<String, String> body = new LinkedHashMap<>();
        body.put("status", up ? "ok" : "degraded");
        body.put("database", up ? "ok" : "unavailable");
        body.put("version", properties.version());
        return ResponseEntity.status(up ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }
}
