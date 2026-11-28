package edu.iiitb.team007.ledger.service;

import edu.iiitb.team007.ledger.domain.User;
import java.util.UUID;

/** JSON of a user (FR-1.1): user_id, name, email, created_at. */
public record UserView(UUID userId, String name, String email, String createdAt) {

    public static UserView of(User u) {
        return new UserView(u.userId(), u.name(), u.email(), u.createdAt());
    }
}
