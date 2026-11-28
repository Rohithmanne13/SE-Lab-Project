package edu.iiitb.team007.ledger.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body of POST /users (FR-1.1): name 1-100 characters after trimming, email syntactically valid and at most
 * 254 characters. The name is trimmed here, so it is stored and returned trimmed.
 */
public record CreateUserRequest(
        @NotBlank(message = "is required and must not be blank")
        @Size(max = 100, message = "must be at most 100 characters after trimming")
        String name,
        @NotNull(message = "is required")
        @Size(max = 254, message = "must be at most 254 characters")
        @Pattern(regexp = EMAIL, message = "must be a valid email address")
        String email) {

    /** Email syntax: the WHATWG HTML "valid email address" form, with at least one dot in the domain. */
    public static final String EMAIL = "^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?"
            + "(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$";

    public CreateUserRequest {
        name = name == null ? null : name.strip();
    }
}
