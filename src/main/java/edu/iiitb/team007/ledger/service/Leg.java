package edu.iiitb.team007.ledger.service;

import edu.iiitb.team007.ledger.domain.Direction;
import edu.iiitb.team007.ledger.domain.Wallet;

/**
 * One side of a transaction to post: the wallet (as read inside the current write transaction), the direction
 * and the amount.
 */
public record Leg(Wallet wallet, Direction direction, long amount) {
}
