package edu.iiitb.team007.ledger.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import edu.iiitb.team007.ledger.domain.ApiException;
import edu.iiitb.team007.ledger.domain.ErrorCode;
import org.junit.jupiter.api.Test;

/** Paging parameters: limit 1-100 (default 50), offset >= 0 (default 0) (FR-6.2). */
class PagingTest {

    @Test
    void defaultsAndValidValues() {
        assertThat(Paging.parse(null, null)).isEqualTo(new Paging(50, 0));
        assertThat(Paging.parse("1", "0")).isEqualTo(new Paging(1, 0));
        assertThat(Paging.parse("100", "250")).isEqualTo(new Paging(100, 250));
    }

    @Test
    void invalidValuesAreValidationErrors() {
        for (String[] bad : new String[][] {{"0", null}, {"101", null}, {"-1", null}, {"abc", null}, {"1.5", null},
            {"", null}, {null, "-1"}, {null, "x"}, {null, "2.0"}, {"9999999999", null}}) {
            assertThatThrownBy(() -> Paging.parse(bad[0], bad[1])).isInstanceOfSatisfying(ApiException.class,
                    e -> assertThat(e.code()).isEqualTo(ErrorCode.VALIDATION_ERROR));
        }
    }
}
