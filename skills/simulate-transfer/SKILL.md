---
name: simulate-transfer
description: Runs a complete Financial Ledger API workflow and verifies ledger consistency - creates two test users and wallets and funds the source, records the initial balances, sends a transfer twice with the same Idempotency-Key and checks it was applied once, fires two concurrent transfers whose total exceeds the balance and checks exactly one succeeds, then calls reconciliation and prints initial and final balances, PASS or FAIL per step and the reconciliation status. Use after changing money-moving code, before a stage submission, or to demonstrate FR-1 to FR-5 end to end.
---

# simulate-transfer

Implements SRS requirement SK-5 (the product-specific skill of the project statement, section 3). Skill format: [[agent-skills-format]]. What it exercises: idempotent transfers ([[idempotency-key-header-draft]]) and race safety from SQLite write transactions ([[sqlite-transactions]]), as designed in ADR-0002.
Run it from the repository root. It needs JDK 21 and Python 3 (standard library only). By default it starts its own copy of the service on a throwaway database, so it never touches real data (NFR-SAFE-5).

## Inputs

- **Who runs it** (optional, recommended): the Team 007 member, passed as `--by` - one of `Manne Rohith Sai`, `Sunnapu Suhith Reddy`, `Rudraraju Dhanush Varma`, `Abhiram P B S`. With `--by` the run is logged in `brain/log.md` (SB-0).
- **Target** (optional): nothing, to start `target/financial-ledger-api.jar` on a free local port with a temporary database and throwaway random API keys (never printed); or `--base-url http://127.0.0.1:8000` to use a running service - then the keys are read from `LEDGER_CLIENT_KEYS` and `LEDGER_ADMIN_KEYS` (the first key of each). Never type a key on the command line or paste it into an AI tool (SEC-5, AI-4).
- **Jar** (optional): `--jar <path>`; by default `target/financial-ledger-api.jar`.

## Steps

1. **Build the jar** if it is missing or out of date: `sh ./mvnw -B -q -DskipTests package` (Windows: `mvnw.cmd -B -q -DskipTests package`).
2. **Run the skill:**

   ```bash
   python3 skills/simulate-transfer/scripts/simulate_transfer.py --by "<member>"
   ```

   The script performs the five steps of SK-5 in order, against the real HTTP API:
   1. **setup** - creates two users with INR wallets and funds the source with an ADMIN deposit of 100000 (INR 1000.00);
   2. **initial** - records both balances with `GET /wallets/{walletId}` (expected 100000 and 0);
   3. **idempotent** - sends a transfer of 25000 with an Idempotency-Key and resends it with the same key: the resend must answer 201 with `Idempotent-Replayed: true` and the same body, and the balances must have changed once (75000 and 25000) (FR-2.6);
   4. **concurrent** - fires two transfers of 50000 from the source at the same instant (100000 > 75000): exactly one must succeed and the other fail with 422 `INSUFFICIENT_FUNDS`; the source ends at 25000 (FR-3.3, FR-3.5);
   5. **reconcile** - `GET /admin/reconciliation` must report `PASSED` (FR-5); the report then summarises the initial and final balances, each step's PASS/FAIL and the reconciliation status.
3. **Read the report.** The exit status is 0 only when all five steps passed and reconciliation is PASSED.
4. **If you ran it with `--by`**, commit the new `brain/log.md` line with your next pull request (SB-5).

## Outputs

- A report on the terminal (UI-4), e.g.:

  ```text
  simulate-transfer report
  target   local jar, throwaway database
  PASS  setup      2 users, 2 INR wallets, deposit 100000 into the source
  PASS  initial    source 100000, destination 0
  PASS  idempotent first 201, resend 201 replayed=yes same body=yes; balances 75000/25000 changed once
  PASS  concurrent 2 x 50000 from 75000: 1 succeeded, 1 INSUFFICIENT_FUNDS; source now 25000
  PASS  reconcile  PASSED: 3 transactions, 6 entries, 0 discrepancies
  balances (minor units)   initial   final
    source                   100000   25000
    destination                   0   75000
  reconciliation: PASSED
  PASS: 5/5 steps passed
  ```

- Exit status 0 (all passed), 1 (a step failed, or the jar is missing or did not start), 2 (bad argument: unknown member, or `--base-url` without keys).
- With `--by`: one line in `brain/log.md`: `- YYYY-MM-DD HH:MM | simulate-transfer | PASS 5/5 steps, reconciliation PASSED | <member> | <target>, <OS>`.
- With the default target: the service log in a temporary folder (path in the error message if it fails to start).

## Failure handling

- **"jar not found":** build it (step 1). **"the service exited" or "did not answer":** read the service log named in the message - usually JDK 21 is missing (`sudo apt install -y openjdk-21-jdk`) or the port is in use; retry, or pass `--timeout 120` on a slow machine.
- **setup FAIL:** the API refused to create users, wallets or the deposit - the detail shows the HTTP status and error code; with `--base-url`, check that the keys in `LEDGER_CLIENT_KEYS` and `LEDGER_ADMIN_KEYS` are the ones the service was started with. The later steps are not run.
- **idempotent FAIL:** a retried request was posted twice or not replayed - a defect in FR-2; open a GitHub Issue labelled bug with the report and add a regression test before fixing it (TQ-8).
- **concurrent FAIL:** both transfers succeeded or both failed - a race-safety defect in FR-3; treat as above.
- **reconcile FAIL:** the ledger does not balance or the hash chain is broken - call `GET /admin/reconciliation` and read `discrepancies`; never "fix" data by editing the database (BR-4).
- Steps after a failed step are reported as SKIP, never as PASS, but reconciliation always runs; the started service is always stopped.

## Evaluation

Run before each stage submission (TQ-7) and record the results in `docs/testing/skill-evaluations.md`:

1. `python3 skills/simulate-transfer/scripts/simulate_transfer.py --by "<member>"` - all 5 steps PASS and reconciliation PASSED (SK-5 acceptance). CI runs the same command on every push and pull request against the freshly built jar.
2. `python3 -m unittest tests.test_simulate_transfer -v` - the report and exit status against a fake API that behaves correctly, that posts a retried transfer twice, that lets both concurrent transfers through, and whose reconciliation fails; plus the argument checks.
3. `python3 scripts/ci/validate_skills.py` - this SKILL.md's format.
4. End-to-end run in the Stage 4 review.
