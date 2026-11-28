package edu.iiitb.team007.ledger.service;

import edu.iiitb.team007.ledger.persistence.ReconciliationQueries;
import edu.iiitb.team007.ledger.persistence.ReconciliationQueries.CurrencySums;
import edu.iiitb.team007.ledger.persistence.ReconciliationQueries.StoredEntry;
import edu.iiitb.team007.ledger.persistence.ReconciliationQueries.TransactionSums;
import edu.iiitb.team007.ledger.persistence.ReconciliationQueries.WalletSums;
import edu.iiitb.team007.ledger.persistence.TransactionRunner;
import edu.iiitb.team007.ledger.service.ReconciliationReport.CurrencyTotals;
import edu.iiitb.team007.ledger.service.ReconciliationReport.Discrepancy;
import edu.iiitb.team007.ledger.support.HashChain;
import edu.iiitb.team007.ledger.support.Timestamps;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Ledger reconciliation (FR-5, ADR-0003 decision 6). Read-only, in one read transaction - one consistent
 * snapshot, while transfers continue (FR-5.1, NFR-REL-5). It verifies that debits equal credits per currency and
 * in total (FR-5.2), every transaction is balanced with at least two entries in one currency (FR-5.3), every
 * stored balance equals its entries and no non-system wallet is negative (FR-5.4), and the hash chain recomputes
 * in sequence order with no gaps (FR-5.5).
 */
@Service
public class ReconciliationService {

    private final TransactionRunner transactions;
    private final ReconciliationQueries queries;
    private final Clock clock;

    public ReconciliationService(TransactionRunner transactions, ReconciliationQueries queries, Clock clock) {
        this.transactions = transactions;
        this.queries = queries;
        this.clock = clock;
    }

    public ReconciliationReport reconcile() {
        return transactions.inReadTransaction(() -> {
            List<Discrepancy> found = new ArrayList<>();
            long txns = queries.countTransactions();
            long entries = queries.countEntries();
            long walletCount = queries.countWallets();
            List<CurrencyTotals> totals = new ArrayList<>();
            long debits = 0;
            long credits = 0;
            for (CurrencySums s : queries.currencySums()) {
                totals.add(new CurrencyTotals(s.currency(), s.debits(), s.credits()));
                debits = Math.addExact(debits, s.debits());
                credits = Math.addExact(credits, s.credits());
                if (s.debits() != s.credits()) {
                    found.add(new Discrepancy("CURRENCY_BALANCED", s.currency(), null, null, null, s.debits(),
                            s.credits()));
                }
            }
            if (debits != credits) {
                found.add(new Discrepancy("LEDGER_BALANCED", null, null, null, null, debits, credits));
            }
            checkTransactions(found);
            checkWallets(found);
            checkHashChain(found);
            return new ReconciliationReport(found.isEmpty() ? ReconciliationReport.PASSED : ReconciliationReport.FAILED,
                    Timestamps.now(clock), txns, entries, walletCount, debits, credits, totals, found);
        });
    }

    private void checkTransactions(List<Discrepancy> found) {
        for (TransactionSums t : queries.transactionSums()) {
            if (t.entryCount() < 2) {
                found.add(new Discrepancy("TRANSACTION_ENTRY_COUNT", t.currency(), null, t.transactionId(), null,
                        "at least 2", t.entryCount()));
            }
            boolean oneCurrency = t.entryCurrencies() <= 1
                    && (t.entryCount() == 0 || t.currency().equals(t.entryCurrency()));
            if (!oneCurrency) {
                found.add(new Discrepancy("TRANSACTION_SINGLE_CURRENCY", t.currency(), null, t.transactionId(), null,
                        t.currency(), t.entryCurrencies() > 1 ? "several currencies" : t.entryCurrency()));
            }
            if (t.debits() != t.credits()) {
                found.add(new Discrepancy("TRANSACTION_BALANCED", t.currency(), null, t.transactionId(), null,
                        t.debits(), t.credits()));
            }
        }
    }

    private void checkWallets(List<Discrepancy> found) {
        for (WalletSums w : queries.walletSums()) {
            if (w.storedBalance() != w.entryBalance()) {
                found.add(new Discrepancy("WALLET_BALANCE", null, w.walletId(), null, null, w.entryBalance(),
                        w.storedBalance()));
            }
            if (!w.system() && w.storedBalance() < 0) {
                found.add(new Discrepancy("WALLET_NOT_NEGATIVE", null, w.walletId(), null, null, "at least 0",
                        w.storedBalance()));
            }
        }
    }

    private void checkHashChain(List<Discrepancy> found) {
        ChainCheck chain = new ChainCheck(found);
        queries.forEachEntryInSequence(chain::check);
    }

    /** Walks the entries in sequence order, carrying the expected sequence and previous hash. */
    private static final class ChainCheck {

        private final List<Discrepancy> found;
        private long expectedSequence = 1;
        private String previousHash = HashChain.GENESIS;

        ChainCheck(List<Discrepancy> found) {
            this.found = found;
        }

        void check(StoredEntry e) {
            if (e.sequence() != expectedSequence) {
                found.add(new Discrepancy("SEQUENCE_GAP", null, null, e.transactionId(), e.entryId(),
                        expectedSequence, e.sequence()));
            }
            if (!previousHash.equals(e.prevHash())) {
                found.add(new Discrepancy("PREV_HASH", null, null, e.transactionId(), e.entryId(), previousHash,
                        e.prevHash()));
            }
            String recomputed = HashChain.entryHash(e.prevHash(), e.sequence(), e.entryId(), e.transactionId(),
                    e.walletId(), e.direction(), e.amount(), e.currency(), e.createdAt());
            if (!recomputed.equals(e.entryHash())) {
                found.add(new Discrepancy("ENTRY_HASH", null, null, e.transactionId(), e.entryId(), recomputed,
                        e.entryHash()));
            }
            expectedSequence = e.sequence() + 1;
            previousHash = e.entryHash();
        }
    }
}
