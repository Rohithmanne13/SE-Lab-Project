package edu.iiitb.team007.ledger.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** SHA-256 against the FIPS 180-4 example "abc" (TQ-1). */
class Sha256Test {

    @Test
    void hashesAbcToTheStandardVector() {
        assertThat(Sha256.hex("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }
}
