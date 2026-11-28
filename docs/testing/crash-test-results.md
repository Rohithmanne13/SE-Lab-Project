# Crash test results (NFR-SAFE-3, NFR-SAFE-4, NFR-REL-2)

Measured with `python3 scripts/nfr/crash_test.py` on 2026-11-28 18:02.

Machine: Linux 6.18.58.1-microsoft-standard-WSL2 (x86_64), 24 CPUs, Python 3.10.12, openjdk version "21.0.13.1" 2026-11-17.

8 clients send transfers of 1 continuously; after 3 s the service process is killed hard (no shutdown), then restarted on the same database file. After each restart every acknowledged transaction is read back, every key that was in flight is retried with the same body, and reconciliation is run.

| Requirement | Target | Measured | Result |
|---|---|---|---|
| NFR-SAFE-3 | all or nothing after a crash: reconciliation PASSED after every restart | 3 hard kills under load of 8 writers | PASS |
| NFR-SAFE-4 | every 201 before the kill survives | 1644 acknowledged, 0 lost | PASS |
| NFR-REL-2 | a restart suffices | restart to /health in 11.8, 12.6, 12.0 s | PASS |
| FR-2.8 after a crash | each in-flight key posts exactly once when retried | 1668 keys retried, 1668 transfers in the ledger, 0 mismatches | PASS |

Overall: PASS
