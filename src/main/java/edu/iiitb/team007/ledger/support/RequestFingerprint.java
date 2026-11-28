package edu.iiitb.team007.ledger.support;

/**
 * Request fingerprint stored with an idempotency key (FR-2.4, ADR-0002):
 * lower-case hex SHA-256 of UTF-8 {@code METHOD + "\n" + path + "\n" + canonical body}; the path has no
 * query string.
 */
public final class RequestFingerprint {

    private RequestFingerprint() {
    }

    public static String of(String method, String path, String canonicalBody) {
        return Sha256.hex(method + "\n" + path + "\n" + canonicalBody);
    }
}
