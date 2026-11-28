#!/usr/bin/env python3
"""Load and timing tests of SRS 5.1 (NFR-PERF-1 to NFR-PERF-5); results go to the test documentation.

  NFR-PERF-1  10 concurrent clients transferring between distinct wallets: p95 <= 250 ms for POST /transfers and
              <= 100 ms for GET /wallets/{walletId}
  NFR-PERF-2  >= 50 committed transfers per second
  NFR-PERF-3  the FR-3.4 scenario (20 concurrent transfers of 100 from a wallet of 1000): every request within 10 s,
              no 5xx (and exactly 10 succeed)
  NFR-PERF-4  reconciliation over >= 100 000 ledger entries within 10 s (and PASSED)
  NFR-PERF-5  ready to serve /health within 15 s of start on an existing database

For NFR-PERF-4 the database is filled to 100 000 entries while the service is stopped, with balanced transfers and
hash-chained entries written by this script's own implementation of FR-4.4; the service then has to reconcile them
as PASSED, which also cross-checks the Java hash chain.

Usage (from the repository root, after `sh ./mvnw -B -DskipTests package`):
  python3 scripts/nfr/load_test.py [--out docs/testing/performance-results.md] [--requests 100]
Exit status 0 only if every requirement passed. Standard library only.
"""
from __future__ import annotations

import argparse
import datetime as dt
import sqlite3
import sys
import tempfile
import threading
import time
import uuid
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import nfr_common as nc  # noqa: E402

CLIENTS = 10
TARGET_ENTRIES = 100_000


def concurrent_transfers(service: nc.Service, pairs: list[tuple[str, str]], per_client: int):
    """NFR-PERF-1/2: each client sends per_client transfers of 1 between its own pair, then reads its wallet."""
    post, get, statuses = [], [], []
    lock = threading.Lock()
    barrier = threading.Barrier(len(pairs))

    def client(src: str, dst: str) -> None:
        c = nc.Client(service)
        mine_post, mine_get, mine_status = [], [], []
        barrier.wait()
        for _ in range(per_client):
            s, _, _, t = c.call("POST", "/api/v1/transfers", body=nc.transfer_body(src, dst, 1), key=nc.new_key())
            mine_post.append(t)
            mine_status.append(s)
        for _ in range(per_client):
            s, _, _, t = c.call("GET", f"/api/v1/wallets/{src}")
            mine_get.append(t)
        c.close()
        with lock:
            post.extend(mine_post)
            get.extend(mine_get)
            statuses.extend(mine_status)

    threads = [threading.Thread(target=client, args=p) for p in pairs]
    t0 = time.perf_counter()
    for t in threads:
        t.start()
    for t in threads:
        t.join()
    total = time.perf_counter() - t0
    return post, get, statuses, total


def fr_3_4_scenario(service: nc.Service, setup: nc.Client) -> tuple[list[float], list[int], int]:
    """NFR-PERF-3: 20 concurrent transfers of 100 from a wallet holding 1000."""
    source = nc.make_wallet(setup, "perf3-source")
    dest = nc.make_wallet(setup, "perf3-dest")
    nc.fund(setup, source, 1000)
    times, codes = [], []
    lock = threading.Lock()
    barrier = threading.Barrier(20)

    def one() -> None:
        c = nc.Client(service)
        barrier.wait()
        s, _, _, t = c.call("POST", "/api/v1/transfers", body=nc.transfer_body(source, dest, 100), key=nc.new_key())
        c.close()
        with lock:
            times.append(t)
            codes.append(s)

    threads = [threading.Thread(target=one) for _ in range(20)]
    for t in threads:
        t.start()
    for t in threads:
        t.join()
    _, _, w, _ = setup.call("GET", f"/api/v1/wallets/{source}")
    return times, codes, w.get("balance")


def seed_entries(db_path: Path, wallet_a: str, wallet_b: str, target: int) -> int:
    """Adds balanced transfers of 1 between two INR wallets (alternating direction) until the ledger holds at least
    `target` entries; the hash chain continues from the last entry. Returns the number of entries afterwards."""
    db = sqlite3.connect(db_path, isolation_level=None)
    try:
        db.execute("BEGIN IMMEDIATE")
        count, last_seq = db.execute("SELECT count(*), COALESCE(MAX(sequence), 0) FROM ledger_entries").fetchone()
        prev = db.execute("SELECT entry_hash FROM ledger_entries ORDER BY sequence DESC LIMIT 1").fetchone()
        prev = prev[0] if prev else nc.GENESIS
        pairs = max(0, -(-(target - count) // 2))
        if pairs % 2:
            pairs += 1  # an even number of alternating transfers leaves both balances unchanged
        now = nc.rfc3339_now()
        txns, entries = [], []
        seq = last_seq
        for i in range(pairs):
            src, dst = (wallet_a, wallet_b) if i % 2 == 0 else (wallet_b, wallet_a)
            txn = str(uuid.uuid4())
            txns.append((txn, "TRANSFER", "COMPLETED", "INR", 1, "load-test seed", None, now))
            for wallet, direction in ((src, "DEBIT"), (dst, "CREDIT")):
                seq += 1
                eid = str(uuid.uuid4())
                h = nc.entry_hash(prev, seq, eid, txn, wallet, direction, 1, "INR", now)
                entries.append((eid, seq, txn, wallet, direction, 1, "INR", now, prev, h))
                prev = h
        db.executemany("INSERT INTO transactions VALUES (?, ?, ?, ?, ?, ?, ?, ?)", txns)
        db.executemany("INSERT INTO ledger_entries VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", entries)
        db.execute("COMMIT")
        return db.execute("SELECT count(*) FROM ledger_entries").fetchone()[0]
    finally:
        db.close()


def main(argv: list[str]) -> int:
    ap = argparse.ArgumentParser(description="Measure NFR-PERF-1 to NFR-PERF-5.")
    ap.add_argument("--out", default="", help="write the results as Markdown to this file")
    ap.add_argument("--requests", type=int, default=100, help="transfers (and reads) per client for NFR-PERF-1/2")
    a = ap.parse_args(argv[1:])
    if not nc.JAR.is_file():
        print(f"FAIL: {nc.JAR} not found - run: sh ./mvnw -B -DskipTests package")
        return 1
    work = Path(tempfile.mkdtemp(prefix="load-test-"))
    service = nc.Service(work / "ledger.db", work / "service.log")
    rows = []
    try:
        service.start()
        setup = nc.Client(service)
        pairs = []
        for i in range(CLIENTS):
            src, dst = nc.make_wallet(setup, f"perf-src-{i}"), nc.make_wallet(setup, f"perf-dst-{i}")
            nc.fund(setup, src, 1_000_000)
            pairs.append((src, dst))
        post, get, statuses, total = concurrent_transfers(service, pairs, a.requests)
        committed = statuses.count(201)
        p95_post, p95_get = nc.percentile(post, 95) * 1000, nc.percentile(get, 95) * 1000
        rows.append(("NFR-PERF-1 POST /transfers", "p95 <= 250 ms (10 clients)",
                     f"p95 {p95_post:.1f} ms over {len(post)} requests, {committed} committed",
                     p95_post <= 250 and committed == len(post)))
        rows.append(("NFR-PERF-1 GET /wallets/{walletId}", "p95 <= 100 ms (10 clients)",
                     f"p95 {p95_get:.1f} ms over {len(get)} requests", p95_get <= 100))
        rate = committed / total
        rows.append(("NFR-PERF-2", ">= 50 committed transfers/s", f"{rate:.0f} transfers/s ({committed} in {total:.1f} s)",
                     rate >= 50))
        times, codes, final = fr_3_4_scenario(service, setup)
        slowest = max(times)
        ok3 = slowest <= 10 and not any(c >= 500 for c in codes) and codes.count(201) == 10 and final == 0
        rows.append(("NFR-PERF-3", "FR-3.4: each request <= 10 s, no 5xx",
                     f"slowest {slowest * 1000:.0f} ms, {codes.count(201)} x 201, {codes.count(422)} x 422, "
                     f"5xx: {sum(1 for c in codes if c >= 500)}, final balance {final}", ok3))
        seed_a, seed_b = nc.make_wallet(setup, "seed-a"), nc.make_wallet(setup, "seed-b")
        nc.fund(setup, seed_a, 1_000_000)
        nc.fund(setup, seed_b, 1_000_000)
        setup.close()
        service.stop()
        entries = seed_entries(service.db_path, seed_a, seed_b, TARGET_ENTRIES)
        ready = service.start()
        rows.append(("NFR-PERF-5", "/health within 15 s of start on an existing database",
                     f"{ready:.1f} s with {entries} entries", ready <= 15))
        c = nc.Client(service, timeout=120)
        s, _, report, t = c.call("GET", "/api/v1/admin/reconciliation", admin=True)
        c.close()
        ok4 = s == 200 and report.get("status") == "PASSED" and report.get("entries_checked", 0) >= TARGET_ENTRIES
        rows.append(("NFR-PERF-4", "reconciliation of >= 100 000 entries <= 10 s",
                     f"{t:.2f} s for {report.get('entries_checked')} entries, status {report.get('status')}",
                     ok4 and t <= 10))
    except (RuntimeError, OSError) as e:
        rows.append(("load test", "runs to the end", f"error: {e}", False))
    finally:
        service.stop()
    table = nc.markdown_table(rows)
    print(table)
    overall = all(r[3] for r in rows) and len(rows) == 6
    print("PASS: every performance requirement met" if overall else "FAIL: see the table")
    if a.out:
        stamp = dt.datetime.now().strftime("%Y-%m-%d %H:%M")
        Path(a.out).write_text(
            "# Performance results (NFR-PERF-1 to NFR-PERF-5)\n\n"
            f"Measured with `python3 scripts/nfr/load_test.py` on {stamp}.\n\n"
            f"Machine: {nc.machine()}.\n\n"
            "The script starts its own copy of the service (temporary database, throwaway keys). NFR-PERF-1/2 use "
            f"{CLIENTS} concurrent clients, each sending {a.requests} transfers of 1 between its own two wallets and "
            "then reading its wallet; NFR-PERF-4 fills the ledger to 100 000 entries with balanced, hash-chained "
            "transfers while the service is stopped.\n\n"
            + table + "\n\n" + ("Overall: PASS\n" if overall else "Overall: FAIL\n"), encoding="utf-8")
        print(f"results written to {a.out}")
    return 0 if overall else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv))
