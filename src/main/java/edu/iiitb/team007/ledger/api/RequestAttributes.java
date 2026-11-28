package edu.iiitb.team007.ledger.api;

/** Names of the servlet request attributes shared by the filters, controllers and the error handler. */
public final class RequestAttributes {

    /** The X-Request-ID of the current request (FR-0.4). */
    public static final String REQUEST_ID = "ledger.requestId";

    /** The caller's role after authentication (FR-0.1). */
    public static final String ROLE = "ledger.role";

    /** The transaction created or replayed by a money-moving request, for the access log (SEC-7). */
    public static final String TRANSACTION_ID = "ledger.transactionId";

    private RequestAttributes() {
    }
}
