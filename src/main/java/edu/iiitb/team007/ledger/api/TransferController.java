package edu.iiitb.team007.ledger.api;

import edu.iiitb.team007.ledger.service.IdempotencyService;
import edu.iiitb.team007.ledger.service.IdempotentResult;
import edu.iiitb.team007.ledger.service.TransferService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/** POST /transfers - idempotent transfer between two wallets (FR-2, FR-3). Role: CLIENT. */
@RestController
public class TransferController {

    private final TransferService transfers;
    private final JsonBodies bodies;

    public TransferController(TransferService transfers, JsonBodies bodies) {
        this.transfers = transfers;
        this.bodies = bodies;
    }

    @PostMapping("/api/v1/transfers")
    public ResponseEntity<String> transfer(HttpServletRequest request,
                                           @RequestHeader(value = IdempotencyKeys.HEADER, required = false) String key,
                                           @RequestBody(required = false) byte[] body) {
        bodies.requireJson(request);
        String idempotencyKey = IdempotencyKeys.require(key);
        JsonBodies.Parsed<TransferRequest> parsed = bodies.read(body, TransferRequest.class);
        TransferRequest r = parsed.value();
        UUID source = RequestIds.require("source_wallet_id", r.sourceWalletId());
        UUID destination = RequestIds.require("destination_wallet_id", r.destinationWalletId());
        String fingerprint = IdempotencyService.fingerprint(request.getMethod(), request.getRequestURI(),
                parsed.tree());
        IdempotentResult result = transfers.transfer(idempotencyKey, fingerprint, source, destination, r.amount(),
                r.currency(), r.description());
        return MoneyResponses.of(result, request);
    }
}
