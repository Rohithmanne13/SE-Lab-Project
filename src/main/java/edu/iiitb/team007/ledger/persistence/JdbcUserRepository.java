package edu.iiitb.team007.ledger.persistence;

import edu.iiitb.team007.ledger.domain.User;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** {@link UserRepository} on SQLite with parameterised SQL (SI-1, SEC-3). */
@Repository
public class JdbcUserRepository implements UserRepository {

    private final LedgerJdbc db;

    public JdbcUserRepository(LedgerJdbc db) {
        this.db = db;
    }

    @Override
    public Optional<User> find(UUID userId) {
        return db.template().query("SELECT user_id, name, email, is_system, created_at FROM users WHERE user_id = ?",
                JdbcUserRepository::map, userId.toString()).stream().findFirst();
    }

    @Override
    public boolean emailExists(String email) {
        Integer n = db.template().queryForObject("SELECT count(*) FROM users WHERE email = ? COLLATE NOCASE",
                Integer.class, email);
        return n != null && n > 0;
    }

    @Override
    public void insert(User user) {
        db.template().update("INSERT INTO users (user_id, name, email, is_system, created_at) VALUES (?, ?, ?, ?, ?)",
                user.userId().toString(), user.name(), user.email(), user.system() ? 1 : 0, user.createdAt());
    }

    private static User map(ResultSet rs, int row) throws SQLException {
        return new User(UUID.fromString(rs.getString("user_id")), rs.getString("name"), rs.getString("email"),
                rs.getInt("is_system") == 1, rs.getString("created_at"));
    }
}
