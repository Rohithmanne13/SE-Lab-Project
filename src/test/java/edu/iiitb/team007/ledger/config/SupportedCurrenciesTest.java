package edu.iiitb.team007.ledger.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Supported currencies: upper-case ISO 4217 codes, at least INR and USD (FR-1.3). */
class SupportedCurrenciesTest {

    private static SupportedCurrencies of(List<String> codes) {
        return new SupportedCurrencies(new LedgerProperties("x.db", "1", codes));
    }

    @Test
    void defaultSetIsInrAndUsd() {
        SupportedCurrencies c = of(List.of("INR", "USD"));
        assertThat(c.all()).containsExactly("INR", "USD");
        assertThat(c.isSupported("INR")).isTrue();
        assertThat(c.isSupported("EUR")).isFalse();
        assertThat(c.isSupported("inr")).isFalse();
    }

    @Test
    void moreIsoCurrenciesMayBeAdded() {
        assertThat(of(List.of("USD", " EUR ", "INR")).all()).containsExactly("EUR", "INR", "USD");
    }

    @Test
    void rejectsBadConfiguration() {
        assertThatThrownBy(() -> of(List.of("INR"))).hasMessageContaining("INR and USD");
        assertThatThrownBy(() -> of(List.of("INR", "USD", "eur"))).hasMessageContaining("ISO 4217");
        assertThatThrownBy(() -> of(List.of("INR", "USD", "ABC"))).hasMessageContaining("ISO 4217");
        assertThatThrownBy(() -> of(List.of())).hasMessageContaining("INR and USD");
    }
}
