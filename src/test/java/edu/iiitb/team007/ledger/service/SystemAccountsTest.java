package edu.iiitb.team007.ledger.service;

import static org.assertj.core.api.Assertions.assertThat;

import edu.iiitb.team007.ledger.domain.User;
import edu.iiitb.team007.ledger.domain.Wallet;
import edu.iiitb.team007.ledger.persistence.LedgerJdbc;
import edu.iiitb.team007.ledger.persistence.TransactionRunner;
import edu.iiitb.team007.ledger.persistence.UserRepository;
import edu.iiitb.team007.ledger.testsupport.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Exactly one system funding account per supported currency, created at start-up (FR-1.5). */
class SystemAccountsTest extends IntegrationTestSupport {

    @Autowired
    private SystemAccounts systemAccounts;

    @Autowired
    private TransactionRunner transactions;

    @Autowired
    private UserRepository users;

    @Autowired
    private LedgerJdbc db;

    @Test
    void oneSystemWalletPerCurrencyOwnedByASystemUser() {
        for (String currency : new String[] {"INR", "USD"}) {
            Wallet w = transactions.inReadTransaction(() -> systemAccounts.fundingWallet(currency));
            assertThat(w.system()).isTrue();
            assertThat(w.currency()).isEqualTo(currency);
            User owner = transactions.inReadTransaction(() -> users.find(w.userId()).orElseThrow());
            assertThat(owner.system()).isTrue();
            assertThat(owner.email()).isEqualTo("system+" + currency.toLowerCase() + "@ledger.invalid");
        }
        assertThat(transactions.inReadTransaction(() -> db.template().queryForObject(
                "SELECT count(*) FROM wallets WHERE is_system = 1", Integer.class))).isEqualTo(2);
    }

    @Test
    void startingAgainCreatesNoSecondAccount() {
        assertThat(systemAccounts.ensureCreated()).isZero();
    }
}
