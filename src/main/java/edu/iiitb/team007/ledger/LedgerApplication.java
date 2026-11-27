package edu.iiitb.team007.ledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** Entry point of the Financial Ledger and Digital Wallet API (ADR-0001). */
@SpringBootApplication
@ConfigurationPropertiesScan
public class LedgerApplication {

    public static void main(String[] args) {
        SpringApplication.run(LedgerApplication.class, args);
    }
}
