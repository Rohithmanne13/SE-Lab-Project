# Performance results (NFR-PERF-1 to NFR-PERF-5)

Measured with `python3 scripts/nfr/load_test.py` on 2026-11-28 18:00.

Machine: Linux 6.18.58.1-microsoft-standard-WSL2 (x86_64), 24 CPUs, Python 3.10.12, openjdk version "21.0.13.1" 2026-11-17.

The script starts its own copy of the service (temporary database, throwaway keys). NFR-PERF-1/2 use 10 concurrent clients, each sending 100 transfers of 1 between its own two wallets and then reading its wallet; NFR-PERF-4 fills the ledger to 100 000 entries with balanced, hash-chained transfers while the service is stopped.

| Requirement | Target | Measured | Result |
|---|---|---|---|
| NFR-PERF-1 POST /transfers | p95 <= 250 ms (10 clients) | p95 140.5 ms over 1000 requests, 1000 committed | PASS |
| NFR-PERF-1 GET /wallets/{walletId} | p95 <= 100 ms (10 clients) | p95 1.8 ms over 1000 requests | PASS |
| NFR-PERF-2 | >= 50 committed transfers/s | 171 transfers/s (1000 in 5.8 s) | PASS |
| NFR-PERF-3 | FR-3.4: each request <= 10 s, no 5xx | slowest 194 ms, 10 x 201, 10 x 422, 5xx: 0, final balance 0 | PASS |
| NFR-PERF-5 | /health within 15 s of start on an existing database | 12.2 s with 100002 entries | PASS |
| NFR-PERF-4 | reconciliation of >= 100 000 entries <= 10 s | 1.77 s for 100002 entries, status PASSED | PASS |

Overall: PASS
