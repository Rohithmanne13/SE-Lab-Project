package edu.iiitb.team007.ledger.config;

/** Caller roles (SRS 2.3, FR-0.1): ADMIN may call every endpoint, CLIENT every non-administrative one. */
public enum ApiRole {
    CLIENT,
    ADMIN
}
