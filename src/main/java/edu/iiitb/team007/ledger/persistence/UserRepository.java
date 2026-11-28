package edu.iiitb.team007.ledger.persistence;

import edu.iiitb.team007.ledger.domain.User;
import java.util.Optional;
import java.util.UUID;

/** Persistence of users (DR-1, CON-5). Call only inside a {@link TransactionRunner} unit of work. */
public interface UserRepository {

    Optional<User> find(UUID userId);

    /** True if a user with this email exists, compared case-insensitively (FR-1.2). */
    boolean emailExists(String email);

    void insert(User user);
}
