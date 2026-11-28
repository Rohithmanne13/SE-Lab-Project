package edu.iiitb.team007.ledger.service;

import edu.iiitb.team007.ledger.config.SupportedCurrencies;
import edu.iiitb.team007.ledger.domain.User;
import edu.iiitb.team007.ledger.domain.Wallet;
import edu.iiitb.team007.ledger.persistence.MigrationRunner;
import edu.iiitb.team007.ledger.persistence.TransactionRunner;
import edu.iiitb.team007.ledger.persistence.UserRepository;
import edu.iiitb.team007.ledger.persistence.WalletRepository;
import edu.iiitb.team007.ledger.support.IdGenerator;
import edu.iiitb.team007.ledger.support.Timestamps;
import java.time.Clock;
import java.util.Locale;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

/**
 * System funding accounts (FR-1.5): for every supported currency exactly one wallet flagged is_system, owned by
 * an internal system user (email {@code system+<currency>@ledger.invalid}). They are created at start-up, after
 * the migrations, represent money entering from outside, and are the only wallets allowed below zero.
 */
@Component
public class SystemAccounts implements InitializingBean {

    private final TransactionRunner transactions;
    private final UserRepository users;
    private final WalletRepository wallets;
    private final SupportedCurrencies currencies;
    private final IdGenerator ids;
    private final Clock clock;

    /** Takes the {@link MigrationRunner} only so that the schema exists before this bean starts. */
    public SystemAccounts(MigrationRunner migrations, TransactionRunner transactions, UserRepository users,
                          WalletRepository wallets, SupportedCurrencies currencies, IdGenerator ids, Clock clock) {
        this.transactions = transactions;
        this.users = users;
        this.wallets = wallets;
        this.currencies = currencies;
        this.ids = ids;
        this.clock = clock;
    }

    @Override
    public void afterPropertiesSet() {
        ensureCreated();
    }

    /** Creates the missing system funding accounts; returns how many were created. */
    public int ensureCreated() {
        return transactions.inWriteTransaction(() -> {
            int created = 0;
            for (String currency : currencies.all()) {
                if (wallets.findSystemWallet(currency).isPresent()) {
                    continue;
                }
                String now = Timestamps.now(clock);
                User owner = new User(ids.newId(), "System funding account " + currency,
                        "system+" + currency.toLowerCase(Locale.ROOT) + "@ledger.invalid", true, now);
                users.insert(owner);
                wallets.insert(new Wallet(ids.newId(), owner.userId(), currency, 0, true, now));
                created++;
            }
            return created;
        });
    }

    /** The funding wallet of a supported currency; call inside a unit of work. */
    public Wallet fundingWallet(String currency) {
        return wallets.findSystemWallet(currency)
                .orElseThrow(() -> new IllegalStateException("No system funding account for " + currency));
    }
}
