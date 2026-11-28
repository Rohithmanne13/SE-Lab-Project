package edu.iiitb.team007.ledger.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import edu.iiitb.team007.ledger.domain.Direction;
import edu.iiitb.team007.ledger.domain.IdempotencyRecord;
import edu.iiitb.team007.ledger.domain.LedgerEntry;
import edu.iiitb.team007.ledger.domain.LedgerTransaction;
import edu.iiitb.team007.ledger.domain.TransactionType;
import edu.iiitb.team007.ledger.domain.User;
import edu.iiitb.team007.ledger.domain.Wallet;
import edu.iiitb.team007.ledger.support.HashChain;
import edu.iiitb.team007.ledger.testsupport.IntegrationTestSupport;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Repository round trips (SI-1). Every test runs inside a write transaction that is rolled back at the end, so
 * the shared test ledger stays untouched.
 */
class RepositoriesTest extends IntegrationTestSupport {

    @Autowired
    private TransactionRunner transactions;

    @Autowired
    private UserRepository users;

    @Autowired
    private WalletRepository wallets;

    @Autowired
    private LedgerRepository ledger;

    @Autowired
    private IdempotencyRepository idempotency;

    /** Thrown at the end of a test body to roll the unit of work back. */
    private static final class Rollback extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    private void inRolledBackTransaction(Runnable body) {
        assertThatThrownBy(() -> transactions.inWriteTransaction(() -> {
            body.run();
            throw new Rollback();
        })).isInstanceOf(Rollback.class);
    }

    @Test
    void usersAndWalletsRoundTrip() {
        inRolledBackTransaction(() -> {
            User u = new User(UUID.randomUUID(), "Alice", "Alice.Repo@Example.com", false, "2026-11-23T10:15:30.123Z");
            users.insert(u);
            assertThat(users.find(u.userId())).contains(u);
            assertThat(users.emailExists("alice.repo@example.com")).isTrue();
            assertThat(users.emailExists("bob.repo@example.com")).isFalse();
            Wallet w = new Wallet(UUID.randomUUID(), u.userId(), "INR", 0, false, u.createdAt());
            wallets.insert(w);
            assertThat(wallets.find(w.walletId())).contains(w);
            assertThat(wallets.findByUser(u.userId())).containsExactly(w);
            assertThat(wallets.exists(u.userId(), "INR")).isTrue();
            assertThat(wallets.exists(u.userId(), "USD")).isFalse();
            wallets.updateBalance(w.walletId(), 500);
            assertThat(wallets.find(w.walletId()).orElseThrow().balance()).isEqualTo(500);
            assertThatThrownBy(() -> wallets.updateBalance(UUID.randomUUID(), 1))
                    .isInstanceOf(IllegalStateException.class);
        });
    }

    @Test
    void ledgerAndIdempotencyRoundTrip() {
        inRolledBackTransaction(() -> {
            User u = new User(UUID.randomUUID(), "Bob", UUID.randomUUID() + "@example.com", false, "t");
            users.insert(u);
            Wallet a = new Wallet(UUID.randomUUID(), u.userId(), "INR", 0, false, "t");
            wallets.insert(a);
            Wallet sys = wallets.findSystemWallet("INR").orElseThrow();
            long seq = ledger.nextSequence();
            String prev = ledger.lastHash();
            LedgerTransaction t = new LedgerTransaction(UUID.randomUUID(), TransactionType.DEPOSIT, "INR", 100, "d",
                    null, "2026-11-23T10:15:30.123Z");
            ledger.insertTransaction(t);
            LedgerEntry debit = entry(t, sys.walletId(), Direction.DEBIT, seq, prev);
            ledger.insertEntry(debit);
            LedgerEntry credit = entry(t, a.walletId(), Direction.CREDIT, seq + 1, debit.entryHash());
            ledger.insertEntry(credit);
            assertThat(ledger.nextSequence()).isEqualTo(seq + 2);
            assertThat(ledger.lastHash()).isEqualTo(credit.entryHash());
            assertThat(ledger.findTransaction(t.transactionId())).contains(t);
            assertThat(ledger.entriesOf(t.transactionId())).containsExactly(debit, credit);
            assertThat(ledger.entriesOfWallet(a.walletId(), 50, 0)).containsExactly(credit);
            assertThat(ledger.countEntriesOfWallet(a.walletId())).isEqualTo(1);
            assertThat(ledger.reversalOf(t.transactionId())).isEmpty();
            IdempotencyRecord r = new IdempotencyRecord("repo-key-" + UUID.randomUUID(), "f".repeat(64),
                    t.transactionId(), 201, "{\"a\":1}", "t");
            idempotency.insert(r);
            assertThat(idempotency.find(r.key())).contains(r);
            assertThat(idempotency.find("missing-key")).isEmpty();
        });
    }

    private static LedgerEntry entry(LedgerTransaction t, UUID wallet, Direction d, long seq, String prev) {
        LedgerEntry e = new LedgerEntry(UUID.randomUUID(), seq, t.transactionId(), wallet, d, t.amount(), t.currency(),
                t.createdAt(), prev, null);
        return new LedgerEntry(e.entryId(), seq, e.transactionId(), wallet, d, e.amount(), e.currency(), e.createdAt(),
                prev, HashChain.entryHash(e));
    }
}
