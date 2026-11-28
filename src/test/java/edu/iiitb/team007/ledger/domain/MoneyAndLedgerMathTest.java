package edu.iiitb.team007.ledger.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Accounting calculations and ledger balance assertions (TQ-1, FR-1.7, FR-1.8, NFR-REL-3, NFR-REL-6). */
class MoneyAndLedgerMathTest {

    private static final UUID TXN = UUID.randomUUID();
    private static final UUID A = UUID.randomUUID();
    private static final UUID B = UUID.randomUUID();

    private static LedgerEntry entry(UUID wallet, Direction d, long amount, String currency) {
        return new LedgerEntry(UUID.randomUUID(), 1, TXN, wallet, d, amount, currency, "t", "p", "h");
    }

    @Test
    void creditAddsAndDebitSubtractsExactly() {
        assertThat(Money.apply(1000, Direction.CREDIT, 250)).isEqualTo(1250);
        assertThat(Money.apply(1000, Direction.DEBIT, 250)).isEqualTo(750);
        assertThat(Money.apply(0, Direction.DEBIT, 1)).isEqualTo(-1);
    }

    @Test
    void limitIsTwoToTheFiftyThreeMinusOne() {
        assertThat(Money.MAX_AMOUNT).isEqualTo((1L << 53) - 1).isEqualTo(9_007_199_254_740_991L);
        assertThat(Money.withinLimit(Money.MAX_AMOUNT)).isTrue();
        assertThat(Money.withinLimit(-Money.MAX_AMOUNT)).isTrue();
        assertThat(Money.withinLimit(Money.MAX_AMOUNT + 1)).isFalse();
        assertThat(Money.withinLimit(-Money.MAX_AMOUNT - 1)).isFalse();
        assertThat(Money.isValidAmount(1)).isTrue();
        assertThat(Money.isValidAmount(Money.MAX_AMOUNT)).isTrue();
        assertThat(Money.isValidAmount(0)).isFalse();
        assertThat(Money.isValidAmount(-5)).isFalse();
        assertThat(Money.isValidAmount(Money.MAX_AMOUNT + 1)).isFalse();
    }

    @Test
    void arithmeticNeverOverflowsSilently() {
        assertThatThrownBy(() -> Money.apply(Long.MAX_VALUE, Direction.CREDIT, 1))
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    void balanceIsSumOfCreditsMinusSumOfDebits() {
        List<LedgerEntry> entries = List.of(entry(A, Direction.CREDIT, 50_000, "INR"),
                entry(A, Direction.DEBIT, 12_550, "INR"), entry(A, Direction.CREDIT, 1, "INR"));
        assertThat(LedgerMath.total(entries, Direction.CREDIT)).isEqualTo(50_001);
        assertThat(LedgerMath.total(entries, Direction.DEBIT)).isEqualTo(12_550);
        assertThat(LedgerMath.balance(entries)).isEqualTo(37_451);
    }

    @Test
    void workedExampleOfTheSrsBalances() {
        // SRS Appendix B: T1 deposit 50 000, T2 transfer 12 550 - each balanced
        assertThat(LedgerMath.isBalanced(List.of(entry(A, Direction.DEBIT, 50_000, "INR"),
                entry(B, Direction.CREDIT, 50_000, "INR")))).isTrue();
        assertThat(LedgerMath.isBalanced(List.of(entry(A, Direction.DEBIT, 12_550, "INR"),
                entry(B, Direction.CREDIT, 12_550, "INR")))).isTrue();
    }

    @Test
    void unbalancedOrMalformedTransactionsAreRejected() {
        assertThat(LedgerMath.isBalanced(List.of(entry(A, Direction.DEBIT, 100, "INR")))).isFalse();
        assertThat(LedgerMath.isBalanced(List.of())).isFalse();
        assertThat(LedgerMath.isBalanced(List.of(entry(A, Direction.DEBIT, 100, "INR"),
                entry(B, Direction.CREDIT, 99, "INR")))).isFalse();
        assertThat(LedgerMath.isBalanced(List.of(entry(A, Direction.DEBIT, 100, "INR"),
                entry(B, Direction.CREDIT, 100, "USD")))).isFalse();
        assertThat(LedgerMath.isBalanced(List.of(entry(A, Direction.DEBIT, 0, "INR"),
                entry(B, Direction.CREDIT, 0, "INR")))).isFalse();
        assertThat(LedgerMath.isBalanced(List.of(entry(A, Direction.CREDIT, 100, "INR"),
                entry(B, Direction.CREDIT, 100, "INR")))).isFalse();
    }

    @Test
    void directionOppositeIsUsedForReversals() {
        assertThat(Direction.DEBIT.opposite()).isEqualTo(Direction.CREDIT);
        assertThat(Direction.CREDIT.opposite()).isEqualTo(Direction.DEBIT);
    }
}
