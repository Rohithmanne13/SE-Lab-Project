package edu.iiitb.team007.ledger.api;

import edu.iiitb.team007.ledger.service.ReconciliationReport;
import edu.iiitb.team007.ledger.service.ReconciliationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /admin/reconciliation (FR-5): runs reconciliation and returns the report with 200, also when its status is
 * FAILED (FR-5.6). Role: ADMIN only (FR-5.7), enforced by {@link ApiKeyFilter}.
 */
@RestController
public class AdminController {

    private final ReconciliationService reconciliation;

    public AdminController(ReconciliationService reconciliation) {
        this.reconciliation = reconciliation;
    }

    @GetMapping("/api/v1/admin/reconciliation")
    public ReconciliationReport reconcile() {
        return reconciliation.reconcile();
    }
}
