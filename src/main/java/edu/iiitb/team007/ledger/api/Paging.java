package edu.iiitb.team007.ledger.api;

import edu.iiitb.team007.ledger.domain.ApiException;
import edu.iiitb.team007.ledger.domain.ErrorCode;
import edu.iiitb.team007.ledger.domain.FieldProblem;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Paging parameters of GET /wallets/{walletId}/entries (FR-6.2): limit 1-100 (default 50) and offset >= 0
 * (default 0), written as plain decimal integers; anything else is 400 VALIDATION_ERROR.
 *
 * @param limit  page size
 * @param offset entries to skip
 */
public record Paging(int limit, int offset) {

    public static final int DEFAULT_LIMIT = 50;
    public static final int MAX_LIMIT = 100;

    private static final Pattern DIGITS = Pattern.compile("^[0-9]{1,9}$");

    public static Paging parse(String limit, String offset) {
        List<FieldProblem> problems = new ArrayList<>();
        int l = DEFAULT_LIMIT;
        int o = 0;
        if (limit != null) {
            boolean valid = DIGITS.matcher(limit).matches() && Integer.parseInt(limit) >= 1
                    && Integer.parseInt(limit) <= MAX_LIMIT;
            if (!valid) {
                problems.add(new FieldProblem("limit", "must be an integer from 1 to 100"));
            } else {
                l = Integer.parseInt(limit);
            }
        }
        if (offset != null) {
            if (!DIGITS.matcher(offset).matches()) {
                problems.add(new FieldProblem("offset", "must be an integer of 0 or more"));
            } else {
                o = Integer.parseInt(offset);
            }
        }
        if (!problems.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "The paging parameters are invalid; see errors.",
                    problems);
        }
        return new Paging(l, o);
    }
}
