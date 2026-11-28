package edu.iiitb.team007.ledger.service;

import edu.iiitb.team007.ledger.domain.ApiException;
import edu.iiitb.team007.ledger.domain.ErrorCode;
import edu.iiitb.team007.ledger.domain.User;
import edu.iiitb.team007.ledger.persistence.SqliteErrors;
import edu.iiitb.team007.ledger.persistence.TransactionRunner;
import edu.iiitb.team007.ledger.persistence.UserRepository;
import edu.iiitb.team007.ledger.support.IdGenerator;
import edu.iiitb.team007.ledger.support.Timestamps;
import java.time.Clock;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

/** Users (FR-1.1, FR-1.2, FR-6.1). */
@Service
public class UserService {

    private final TransactionRunner transactions;
    private final UserRepository users;
    private final IdGenerator ids;
    private final Clock clock;

    public UserService(TransactionRunner transactions, UserRepository users, IdGenerator ids, Clock clock) {
        this.transactions = transactions;
        this.users = users;
        this.ids = ids;
        this.clock = clock;
    }

    /** Creates a user; a duplicate email, compared case-insensitively, is 409 EMAIL_ALREADY_EXISTS. */
    public UserView create(String name, String email) {
        try {
            return transactions.inWriteTransaction(() -> {
                if (users.emailExists(email)) {
                    throw emailExists();
                }
                User user = new User(ids.newId(), name, email, false, Timestamps.now(clock));
                users.insert(user);
                return UserView.of(user);
            });
        } catch (DataAccessException e) {
            if (SqliteErrors.isConstraintViolation(e)) {
                throw emailExists();
            }
            throw e;
        }
    }

    /** A user; system users are internal and reported as not found (FR-1.5). */
    public UserView get(UUID userId) {
        return transactions.inReadTransaction(() -> users.find(userId).filter(u -> !u.system())
                .map(UserView::of).orElseThrow(() -> notFound(userId)));
    }

    static ApiException notFound(UUID userId) {
        return new ApiException(ErrorCode.USER_NOT_FOUND, "No user with id " + userId + " exists.");
    }

    private static ApiException emailExists() {
        return new ApiException(ErrorCode.EMAIL_ALREADY_EXISTS,
                "A user with this email already exists (emails are compared ignoring case); use another email.");
    }
}
