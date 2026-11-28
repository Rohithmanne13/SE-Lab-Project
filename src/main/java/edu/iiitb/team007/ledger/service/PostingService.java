package edu.iiitb.team007.ledger.service;

import edu.iiitb.team007.ledger.domain.ApiException;
import edu.iiitb.team007.ledger.domain.Direction;
import edu.iiitb.team007.ledger.domain.ErrorCode;
import edu.iiitb.team007.ledger.domain.LedgerEntry;
import edu.iiitb.team007.ledger.domain.LedgerMath;
import edu.iiitb.team007.ledger.domain.LedgerTransaction;
import edu.iiitb.team007.ledger.domain.Money;
import edu.iiitb.team007.ledger.domain.TransactionType;
import edu.iiitb.team007.ledger.domain.Wallet;
import edu.iiitb.team007.ledger.persistence.LedgerRepository;
import edu.iiitb.team007.ledger.persistence.WalletRepository;
import edu.iiitb.team007.ledger.support.HashChain;
import edu.iiitb.team007.ledger.support.IdGenerator;
import edu.iiitb.team007.ledger.support.Timestamps;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Posts one balanced ledger transaction inside the caller's write transaction (ADR-0002 step 3, ADR-0003):
 * hash-chained entries with the next sequence numbers, then the new stored balances. It is the only code that
 * changes a balance, so every balance is backed by entries (FR-1.7). It refuses an unbalanced transaction
 * (FR-1.8: 500, nothing persisted), a negative non-system balance (BR-3) and a balance beyond 2^53 - 1
 * (NFR-REL-6).
 */
@Service
public class PostingService {

    /** A posted transaction and its entries. */
    public record Posted(LedgerTransaction transaction, List<LedgerEntry> entries) {
    }

    private final LedgerRepository ledger;
    private final WalletRepository wallets;
    private final IdGenerator ids;
    private final Clock clock;

    public PostingService(LedgerRepository ledger, WalletRepository wallets, IdGenerator ids, Clock clock) {
        this.ledger = ledger;
        this.wallets = wallets;
        this.ids = ids;
        this.clock = clock;
    }

    public Posted post(TransactionType type, String currency, long amount, String description,
                       UUID reversesTransactionId, List<Leg> legs) {
        Map<UUID, Long> newBalances = newBalances(legs);
        String now = Timestamps.now(clock);
        LedgerTransaction transaction = new LedgerTransaction(ids.newId(), type, currency, amount, description,
                reversesTransactionId, now);
        List<LedgerEntry> entries = chainedEntries(transaction, ordered(legs));
        if (!LedgerMath.isBalanced(entries) || legs.stream().anyMatch(l -> !l.wallet().currency().equals(currency))) {
            throw new IllegalStateException("Refusing to commit an unbalanced or mixed-currency transaction (FR-1.8)");
        }
        ledger.insertTransaction(transaction);
        for (LedgerEntry e : entries) {
            ledger.insertEntry(e);
        }
        newBalances.forEach(wallets::updateBalance);
        return new Posted(transaction, entries);
    }

    /** Balances after the legs; checks BR-3 and NFR-REL-6 before anything is written. */
    private static Map<UUID, Long> newBalances(List<Leg> legs) {
        Map<UUID, Long> balances = new LinkedHashMap<>();
        Map<UUID, Wallet> byId = new LinkedHashMap<>();
        for (Leg leg : legs) {
            if (!Money.isValidAmount(leg.amount())) {
                throw new IllegalStateException("Entry amount out of range: " + leg.amount());
            }
            byId.put(leg.wallet().walletId(), leg.wallet());
            long current = balances.getOrDefault(leg.wallet().walletId(), leg.wallet().balance());
            balances.put(leg.wallet().walletId(), Money.apply(current, leg.direction(), leg.amount()));
        }
        balances.forEach((id, balance) -> {
            Wallet w = byId.get(id);
            if (!w.system() && balance < 0) {
                throw new ApiException(ErrorCode.INSUFFICIENT_FUNDS, "Wallet " + id + " holds " + w.balance()
                        + " but this needs " + (w.balance() - balance) + "; nothing was changed.");
            }
            if (!Money.withinLimit(balance)) {
                throw new ApiException(ErrorCode.AMOUNT_LIMIT_EXCEEDED, "A balance would exceed "
                        + Money.MAX_AMOUNT + " minor units (2^53 - 1); nothing was changed.");
            }
        });
        return balances;
    }

    /** DEBIT entries first, then CREDIT entries, each in wallet order - a reproducible chain (ADR-0003). */
    private static List<Leg> ordered(List<Leg> legs) {
        List<Leg> sorted = new ArrayList<>(legs);
        sorted.sort(Comparator.comparing((Leg l) -> l.direction() == Direction.DEBIT ? 0 : 1)
                .thenComparing(l -> l.wallet().walletId().toString()));
        return sorted;
    }

    private List<LedgerEntry> chainedEntries(LedgerTransaction t, List<Leg> legs) {
        long sequence = ledger.nextSequence();
        String prevHash = ledger.lastHash();
        List<LedgerEntry> entries = new ArrayList<>();
        for (Leg leg : legs) {
            LedgerEntry unhashed = new LedgerEntry(ids.newId(), sequence, t.transactionId(), leg.wallet().walletId(),
                    leg.direction(), leg.amount(), t.currency(), t.createdAt(), prevHash, null);
            String hash = HashChain.entryHash(unhashed);
            entries.add(new LedgerEntry(unhashed.entryId(), sequence, t.transactionId(), leg.wallet().walletId(),
                    leg.direction(), leg.amount(), t.currency(), t.createdAt(), prevHash, hash));
            prevHash = hash;
            sequence++;
        }
        return entries;
    }
}
