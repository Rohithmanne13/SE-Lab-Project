package edu.iiitb.team007.ledger.api;

import edu.iiitb.team007.ledger.service.QueryService;
import edu.iiitb.team007.ledger.service.TransactionView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /transactions/{transactionId} (FR-6.3, FR-4.7). There is no PUT, PATCH or DELETE on transactions or
 * entries (FR-4.1). Role: CLIENT.
 */
@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final QueryService queries;

    public TransactionController(QueryService queries) {
        this.queries = queries;
    }

    @GetMapping("/{transactionId}")
    public TransactionView get(@PathVariable String transactionId) {
        return queries.transaction(RequestIds.require("transactionId", transactionId));
    }
}
