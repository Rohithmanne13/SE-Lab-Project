package edu.iiitb.team007.ledger.api;

import edu.iiitb.team007.ledger.service.EntryPageView;
import edu.iiitb.team007.ledger.service.QueryService;
import edu.iiitb.team007.ledger.service.WalletListView;
import edu.iiitb.team007.ledger.service.WalletService;
import edu.iiitb.team007.ledger.service.WalletView;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Wallets: POST and GET /users/{userId}/wallets (FR-1.3, FR-1.4, FR-6.1), GET /wallets/{walletId} (FR-6.1) and
 * GET /wallets/{walletId}/entries (FR-6.2). Role: CLIENT.
 */
@RestController
@RequestMapping("/api/v1")
public class WalletController {

    private final WalletService walletService;
    private final QueryService queries;
    private final JsonBodies bodies;

    public WalletController(WalletService walletService, QueryService queries, JsonBodies bodies) {
        this.walletService = walletService;
        this.queries = queries;
        this.bodies = bodies;
    }

    @PostMapping("/users/{userId}/wallets")
    public ResponseEntity<WalletView> create(HttpServletRequest request, @PathVariable String userId,
                                             @RequestBody(required = false) byte[] body) {
        bodies.requireJson(request);
        UUID id = RequestIds.require("userId", userId);
        CreateWalletRequest r = bodies.read(body, CreateWalletRequest.class).value();
        WalletView wallet = walletService.create(id, r.currency());
        return Created.at("/api/v1/wallets/" + wallet.walletId(), wallet);
    }

    @GetMapping("/users/{userId}/wallets")
    public WalletListView list(@PathVariable String userId) {
        return walletService.list(RequestIds.require("userId", userId));
    }

    @GetMapping("/wallets/{walletId}")
    public WalletView get(@PathVariable String walletId) {
        return queries.wallet(RequestIds.require("walletId", walletId));
    }

    @GetMapping("/wallets/{walletId}/entries")
    public EntryPageView entries(@PathVariable String walletId, @RequestParam(required = false) String limit,
                                 @RequestParam(required = false) String offset) {
        UUID id = RequestIds.require("walletId", walletId);
        Paging paging = Paging.parse(limit, offset);
        return queries.entries(id, paging.limit(), paging.offset());
    }
}
