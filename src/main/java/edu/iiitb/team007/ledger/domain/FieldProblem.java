package edu.iiitb.team007.ledger.domain;

/**
 * One invalid field of a request, listed in the {@code errors} member of a VALIDATION_ERROR problem
 * (SRS 3.3.4, UI-3).
 *
 * @param field   the field, header or parameter name, e.g. {@code amount}
 * @param message what is wrong with it, in one short phrase
 */
public record FieldProblem(String field, String message) {
}
