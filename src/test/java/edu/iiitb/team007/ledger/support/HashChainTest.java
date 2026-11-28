package edu.iiitb.team007.ledger.support;

import static org.assertj.core.api.Assertions.assertThat;

import edu.iiitb.team007.ledger.domain.Direction;
import edu.iiitb.team007.ledger.domain.LedgerEntry;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** The documented hash serialisation of FR-4.4 / ADR-0003, pinned by a known vector (TQ-1). */
class HashChainTest {

    private static final UUID ENTRY = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID TXN = UUID.fromString("22222222-2222-4222-8222-222222222222");
    private static final UUID WALLET = UUID.fromString("33333333-3333-4333-8333-333333333333");

    @Test
    void genesisIsSixtyFourZeros() {
        assertThat(HashChain.GENESIS).hasSize(64).matches("0+");
    }

    @Test
    void serialisationIsPrevHashNewlineThenPipeJoinedFields() {
        assertThat(HashChain.serialise(HashChain.GENESIS, 1, ENTRY.toString(), TXN.toString(), WALLET.toString(),
                "DEBIT", 12550, "INR", "2026-11-23T10:15:30.123Z"))
                .isEqualTo(HashChain.GENESIS + "\n1|" + ENTRY + "|" + TXN + "|" + WALLET
                        + "|DEBIT|12550|INR|2026-11-23T10:15:30.123Z");
    }

    @Test
    void entryHashMatchesIndependentlyComputedVector() {
        // computed independently with Python hashlib over the same serialisation
        LedgerEntry e = new LedgerEntry(ENTRY, 1, TXN, WALLET, Direction.DEBIT, 12550, "INR",
                "2026-11-23T10:15:30.123Z", HashChain.GENESIS, null);
        assertThat(HashChain.entryHash(e))
                .isEqualTo("faf4baf1ab1d828a3606404f852afbfe4160c8a68db4f4758f256de05a14cec2");
    }

    @Test
    void anyChangedFieldChangesTheHash() {
        String base = HashChain.entryHash(HashChain.GENESIS, 1, "a", "b", "c", "DEBIT", 100, "INR", "t");
        assertThat(HashChain.entryHash(HashChain.GENESIS, 1, "a", "b", "c", "DEBIT", 101, "INR", "t"))
                .isNotEqualTo(base);
        assertThat(HashChain.entryHash(HashChain.GENESIS, 1, "a", "b", "c", "CREDIT", 100, "INR", "t"))
                .isNotEqualTo(base);
        assertThat(HashChain.entryHash("1".repeat(64), 1, "a", "b", "c", "DEBIT", 100, "INR", "t")).isNotEqualTo(base);
        assertThat(base).hasSize(64).matches("[0-9a-f]+");
    }
}
