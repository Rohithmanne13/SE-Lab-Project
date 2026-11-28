package edu.iiitb.team007.ledger.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** The error catalogue matches SRS section 3.3.4 exactly (FR-0.3). */
class ErrorCodeTest {

    @Test
    void everyCodeOfTheSrsHasItsStatus() {
        Map<String, Integer> expected = Map.ofEntries(
                Map.entry("VALIDATION_ERROR", 400), Map.entry("INVALID_ID", 400),
                Map.entry("IDEMPOTENCY_KEY_MISSING", 400), Map.entry("IDEMPOTENCY_KEY_INVALID", 400),
                Map.entry("UNAUTHENTICATED", 401), Map.entry("FORBIDDEN", 403),
                Map.entry("USER_NOT_FOUND", 404), Map.entry("WALLET_NOT_FOUND", 404),
                Map.entry("TRANSACTION_NOT_FOUND", 404), Map.entry("EMAIL_ALREADY_EXISTS", 409),
                Map.entry("WALLET_ALREADY_EXISTS", 409), Map.entry("TRANSACTION_ALREADY_REVERSED", 409),
                Map.entry("PAYLOAD_TOO_LARGE", 413), Map.entry("UNSUPPORTED_MEDIA_TYPE", 415),
                Map.entry("INSUFFICIENT_FUNDS", 422), Map.entry("CURRENCY_MISMATCH", 422),
                Map.entry("SAME_WALLET_TRANSFER", 422), Map.entry("UNSUPPORTED_CURRENCY", 422),
                Map.entry("TRANSACTION_NOT_REVERSIBLE", 422), Map.entry("IDEMPOTENCY_KEY_REUSED", 422),
                Map.entry("AMOUNT_LIMIT_EXCEEDED", 422), Map.entry("INTERNAL_ERROR", 500),
                Map.entry("SERVICE_BUSY", 503));
        Map<String, Integer> actual = Arrays.stream(ErrorCode.values())
                .collect(Collectors.toMap(Enum::name, ErrorCode::status));
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void typeUriLinksToTheCodesHeading() {
        assertThat(ErrorCode.INSUFFICIENT_FUNDS.typeUri()).isEqualTo("https://github.com/Rohithmanne13/"
                + "SE-Lab-Project/blob/main/docs/design/error-codes.md#insufficient_funds");
        assertThat(ErrorCode.SERVICE_BUSY.title()).isEqualTo("Service busy");
    }

    @Test
    void apiExceptionCarriesCodeDetailAndErrors() {
        ApiException e = new ApiException(ErrorCode.VALIDATION_ERROR, "Fix the amount.",
                java.util.List.of(new FieldProblem("amount", "must be a positive integer")));
        assertThat(e.code()).isEqualTo(ErrorCode.VALIDATION_ERROR);
        assertThat(e.detail()).isEqualTo("Fix the amount.");
        assertThat(e.errors()).containsExactly(new FieldProblem("amount", "must be a positive integer"));
        assertThat(new ApiException(ErrorCode.FORBIDDEN, "x").errors()).isEmpty();
    }
}
