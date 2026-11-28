package edu.iiitb.team007.ledger.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests of posting with substituted providers (SE-6): in-memory repositories, a fixed clock and sequential
 * IDs (TQ-1: accounting calculations and ledger balance assertions; FR-1.7, FR-1.8, FR-4.3, FR-4.4).
 */
class PostingServiceTest {

    private final FakeLedger ledger = new FakeLedger();
    private final FakeWallets wallets = new FakeWallets();
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-23T10:15:30.123Z"), ZoneOffset.UTC);
    private long nextId = 1;
    private final IdGenerator ids = () -> new UUID(0L, nextId++);
    private PostingService posting;
    private Wallet system;
    private Wallet alice;
    private Wallet bob;

    @BeforeEach
    void setUp() {
        posting = new PostingService(ledger, wallets, ids, clock);
        system = wallets.add(new Wallet(UUID.randomUUID(), UUID.randomUUID(), "INR", 0, true, "t"));
        alice = wallets.add(new Wallet(UUID.randomUUID(), UUID.randomUUID(), "INR", 0, false, "t"));
        bob = wallets.add(new Wallet(UUID.randomUUID(), UUID.randomUUID(), "INR", 0, false, "t"));
    }

    @Test
    void workedExampleOfTheSrs() {
        // Appendix B: deposit 50 000 to Alice, then Alice sends 12 550 to Bob
        posting.post(TransactionType.DEPOSIT, "INR", 50_000, null, null, List.of(
                new Leg(system, Direction.DEBIT, 50_000), new Leg(alice, Direction.CREDIT, 50_000)));
        posting.post(TransactionType.TRANSFER, "INR", 12_550, null, null, List.of(
                new Leg(wallets.get(alice), Direction.DEBIT, 12_550), new Leg(bob, Direction.CREDIT, 12_550)));

        assertThat(wallets.get(system).balance()).isEqualTo(-50_000);
        assertThat(wallets.get(alice).balance()).isEqualTo(37_450);
        assertThat(wallets.get(bob).balance()).isEqualTo(12_550);
        assertThat(LedgerMath.total(ledger.entries, Direction.DEBIT))
                .isEqualTo(LedgerMath.total(ledger.entries, Direction.CREDIT)).isEqualTo(62_550);
    }

    @Test
    void entriesAreHashChainedWithGapFreeSequencesDebitFirst() {
        PostingService.Posted p = posting.post(TransactionType.DEPOSIT, "INR", 100, "d", null, List.of(
                new Leg(alice, Direction.CREDIT, 100), new Leg(system, Direction.DEBIT, 100)));
        assertThat(p.entries()).extracting(LedgerEntry::direction).containsExactly(Direction.DEBIT, Direction.CREDIT);
        assertThat(p.entries()).extracting(LedgerEntry::sequence).containsExactly(1L, 2L);
        assertThat(p.entries().get(0).prevHash()).isEqualTo(HashChain.GENESIS);
        assertThat(p.entries().get(1).prevHash()).isEqualTo(p.entries().get(0).entryHash());
        for (LedgerEntry e : p.entries()) {
            assertThat(e.entryHash()).isEqualTo(HashChain.entryHash(e));
            assertThat(e.createdAt()).isEqualTo("2026-11-23T10:15:30.123Z");
        }
        PostingService.Posted next = posting.post(TransactionType.TRANSFER, "INR", 40, null, null, List.of(
                new Leg(wallets.get(alice), Direction.DEBIT, 40), new Leg(bob, Direction.CREDIT, 40)));
        assertThat(next.entries().get(0).sequence()).isEqualTo(3);
        assertThat(next.entries().get(0).prevHash()).isEqualTo(p.entries().get(1).entryHash());
        assertThat(next.transaction().transactionId()).isNotEqualTo(p.transaction().transactionId());
    }

    @Test
    void unbalancedOrSingleEntryOrMixedCurrencyTransactionsAreRefusedAndNothingIsWritten() {
        Wallet usd = wallets.add(new Wallet(UUID.randomUUID(), UUID.randomUUID(), "USD", 0, false, "t"));
        List<List<Leg>> bad = List.of(
                List.of(new Leg(system, Direction.DEBIT, 100), new Leg(alice, Direction.CREDIT, 99)),
                List.of(new Leg(system, Direction.DEBIT, 100)),
                List.of(new Leg(system, Direction.DEBIT, 100), new Leg(usd, Direction.CREDIT, 100)));
        for (List<Leg> legs : bad) {
            assertThatThrownBy(() -> posting.post(TransactionType.DEPOSIT, "INR", 100, null, null, legs))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("FR-1.8");
        }
        assertThat(ledger.transactions).isEmpty();
        assertThat(ledger.entries).isEmpty();
        assertThat(wallets.get(alice).balance()).isZero();
    }

    @Test
    void nonSystemWalletsCannotGoNegativeSystemWalletsCan() {
        assertThatThrownBy(() -> posting.post(TransactionType.TRANSFER, "INR", 1, null, null, List.of(
                new Leg(alice, Direction.DEBIT, 1), new Leg(bob, Direction.CREDIT, 1))))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.INSUFFICIENT_FUNDS));
        posting.post(TransactionType.DEPOSIT, "INR", 1, null, null, List.of(
                new Leg(system, Direction.DEBIT, 1), new Leg(alice, Direction.CREDIT, 1)));
        assertThat(wallets.get(system).balance()).isEqualTo(-1);
    }

    @Test
    void balancesBeyondTwoToTheFiftyThreeMinusOneAreRefused() {
        posting.post(TransactionType.DEPOSIT, "INR", Money.MAX_AMOUNT, null, null, List.of(
                new Leg(system, Direction.DEBIT, Money.MAX_AMOUNT),
                new Leg(alice, Direction.CREDIT, Money.MAX_AMOUNT)));
        assertThat(wallets.get(alice).balance()).isEqualTo(Money.MAX_AMOUNT);
        assertThatThrownBy(() -> posting.post(TransactionType.DEPOSIT, "INR", 1, null, null, List.of(
                new Leg(wallets.get(system), Direction.DEBIT, 1), new Leg(wallets.get(alice), Direction.CREDIT, 1))))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.AMOUNT_LIMIT_EXCEEDED));
        assertThatThrownBy(() -> posting.post(TransactionType.DEPOSIT, "INR", 0, null, null, List.of(
                new Leg(system, Direction.DEBIT, 0), new Leg(bob, Direction.CREDIT, 0))))
                .isInstanceOf(IllegalStateException.class);
    }

    /** In-memory {@link LedgerRepository}. */
    private static final class FakeLedger implements LedgerRepository {
        private final List<LedgerTransaction> transactions = new ArrayList<>();
        private final List<LedgerEntry> entries = new ArrayList<>();

        @Override
        public long nextSequence() {
            return entries.size() + 1L;
        }

        @Override
        public String lastHash() {
            return entries.isEmpty() ? HashChain.GENESIS : entries.get(entries.size() - 1).entryHash();
        }

        @Override
        public void insertTransaction(LedgerTransaction transaction) {
            transactions.add(transaction);
        }

        @Override
        public void insertEntry(LedgerEntry entry) {
            entries.add(entry);
        }

        @Override
        public Optional<LedgerTransaction> findTransaction(UUID transactionId) {
            return transactions.stream().filter(t -> t.transactionId().equals(transactionId)).findFirst();
        }

        @Override
        public List<LedgerEntry> entriesOf(UUID transactionId) {
            return entries.stream().filter(e -> e.transactionId().equals(transactionId)).toList();
        }

        @Override
        public Optional<UUID> reversalOf(UUID transactionId) {
            return Optional.empty();
        }

        @Override
        public List<LedgerEntry> entriesOfWallet(UUID walletId, int limit, int offset) {
            return entries.stream().filter(e -> e.walletId().equals(walletId)).skip(offset).limit(limit).toList();
        }

        @Override
        public long countEntriesOfWallet(UUID walletId) {
            return entries.stream().filter(e -> e.walletId().equals(walletId)).count();
        }
    }

    /** In-memory {@link WalletRepository}. */
    private static final class FakeWallets implements WalletRepository {
        private final Map<UUID, Wallet> wallets = new LinkedHashMap<>();

        Wallet add(Wallet w) {
            wallets.put(w.walletId(), w);
            return w;
        }

        Wallet get(Wallet w) {
            return wallets.get(w.walletId());
        }

        @Override
        public Optional<Wallet> find(UUID walletId) {
            return Optional.ofNullable(wallets.get(walletId));
        }

        @Override
        public Optional<Wallet> findSystemWallet(String currency) {
            return wallets.values().stream().filter(w -> w.system() && w.currency().equals(currency)).findFirst();
        }

        @Override
        public List<Wallet> findByUser(UUID userId) {
            return wallets.values().stream().filter(w -> w.userId().equals(userId)).toList();
        }

        @Override
        public boolean exists(UUID userId, String currency) {
            return findByUser(userId).stream().anyMatch(w -> w.currency().equals(currency));
        }

        @Override
        public void insert(Wallet wallet) {
            add(wallet);
        }

        @Override
        public void updateBalance(UUID walletId, long balance) {
            Wallet w = wallets.get(walletId);
            wallets.put(walletId,
                    new Wallet(w.walletId(), w.userId(), w.currency(), balance, w.system(), w.createdAt()));
        }
    }
}
