package edu.iiitb.team007.ledger.persistence;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Checks database availability with a read-only trivial query; never writes (FR-6.4). */
@Component
public class JdbcDatabaseHealth implements DatabaseHealth {

    private final JdbcTemplate jdbc;

    public JdbcDatabaseHealth(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean isAvailable() {
        try {
            Integer one = jdbc.queryForObject("SELECT 1", Integer.class);
            return one != null && one == 1;
        } catch (DataAccessException e) {
            return false;
        }
    }
}
