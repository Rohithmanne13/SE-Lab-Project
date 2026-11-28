package edu.iiitb.team007.ledger.domain;

import java.util.List;

/**
 * A request that cannot be served, with the stable error code that decides the HTTP status (ADR-0004).
 * Thrown by every layer; converted to an RFC 9457 problem document by the API layer.
 */
public class ApiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ErrorCode code;
    private final transient List<FieldProblem> errors;

    /**
     * @param code   the error code (SRS 3.3.4)
     * @param detail one sentence telling the caller what to correct (UI-3); never internal details (SEC-6)
     */
    public ApiException(ErrorCode code, String detail) {
        this(code, detail, List.of());
    }

    public ApiException(ErrorCode code, String detail, List<FieldProblem> errors) {
        super(detail);
        this.code = code;
        this.errors = List.copyOf(errors);
    }

    public ErrorCode code() {
        return code;
    }

    public String detail() {
        return getMessage();
    }

    /** Offending fields; non-empty only for VALIDATION_ERROR. */
    public List<FieldProblem> errors() {
        return errors;
    }
}
