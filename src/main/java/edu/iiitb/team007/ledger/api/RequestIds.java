package edu.iiitb.team007.ledger.api;

import edu.iiitb.team007.ledger.domain.ApiException;
import edu.iiitb.team007.ledger.domain.ErrorCode;
import edu.iiitb.team007.ledger.support.Uuids;
import java.util.UUID;

/** Identifiers from paths and bodies; a malformed one is 400 INVALID_ID before any database access (FR-1.9). */
public final class RequestIds {

    private RequestIds() {
    }

    /**
     * @param name  the parameter or field name used in the error detail, e.g. {@code walletId}
     * @param value the text received
     */
    public static UUID require(String name, String value) {
        return Uuids.parse(value).orElseThrow(() -> new ApiException(ErrorCode.INVALID_ID,
                "The " + name + " is not a well-formed UUID (for example 3f0c2b1a-9d4e-4c7a-8b2f-1e5d6a7b8c9d)."));
    }
}
