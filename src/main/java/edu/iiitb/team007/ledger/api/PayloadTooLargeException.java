package edu.iiitb.team007.ledger.api;

import java.io.IOException;

/** Raised while reading a body that turns out to be larger than the limit (FR-0.5). */
public class PayloadTooLargeException extends IOException {

    private static final long serialVersionUID = 1L;

    public PayloadTooLargeException() {
        super("Request body larger than " + PayloadSizeFilter.MAX_BYTES + " bytes");
    }
}
