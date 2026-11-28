#!/usr/bin/env python3
"""Crash test of SRS 5.2 / 5.4 (NFR-SAFE-3, NFR-SAFE-4, NFR-REL-2): kill the service hard while transfers are in
flight, restart it on the same database file, and verify that

  NFR-SAFE-3  every transaction is all or nothing: reconciliation PASSED, no half-written transfer
  NFR-SAFE-4  every transfer acknowledged with 201 before the kill is still there after the restart
  NFR-REL-2   a plain restart is enough: SQLite recovery restores the last committed state
  FR-2.8      retrying every key that was in flight posts each transfer exactly once (replay or new post)

Usage (from the repository root, after `sh ./mvnw -B -DskipTests package`):
  python3 scripts/nfr/crash_test.py [--out docs/testing/crash-test-results.md] [--crashes 3]
Exit status 0 only if every check passed. Standard library only.
"""
from __future__ import annotations

import argparse
import datetime as dt
import sys
import tempfile
import threading
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import nfr_common as nc  # noqa: E402

WRITERS = 8
FUNDING = 1_000_000


def load_until_killed(service: nc.Service, pairs: list[tuple[str, str]], run_seconds: float):
    """WRITERS threads send transfers of 1 until the service dies; returns (acknowledged, attempted) where
    acknowledged maps key -> transaction_id of every 201 and attempted maps key -> (source, destination)."""
    acknowledged: dict[str, str] = {}
    attempted: dict[str, tuple[str, str]] = {}
    lock = threading.Lock()
    stop = threading.Event()

    def writer(i: int) -> None:
        src, dst = pairs[i % len(pairs)]
        c = nc.Client(service, timeout=10)
        while not stop.is_set():
            key = nc.new_key()
            with lock:
                attempted[key] = (src, dst)
            try:
                s, _, body, _ = c.call("POST", "/api/v1/transfers", body=nc.transfer_body(src, dst, 1), key=key)
            except OSError:
                return
            except Exception:  # noqa: BLE001 - any client error once the server is gone ends this writer
                return
            if s == 201:
                with lock:
                    acknowledged[key] = body["transaction_id"]

    threads = [threading.Thread(target=writer, args=(i,)) for i in range(WRITERS)]
    for t in threads:
        t.start()
    time.sleep(run_seconds)
    service.kill()
    stop.set()
    for t in threads:
        t.join(timeout=30)
    return acknowledged, attempted


def main(argv: list[str]) -> int:
    ap = argparse.ArgumentParser(description="Kill the service under load and verify recovery.")
    ap.add_argument("--out", default="", help="write the results as Markdown to this file")
    ap.add_argument("--crashes", type=int, default=3, help="how many kill-and-restart cycles")
    ap.add_argument("--seconds", type=float, default=3.0, help="seconds of load before each kill")
    a = ap.parse_args(argv[1:])
    if not nc.JAR.is_file():
        print(f"FAIL: {nc.JAR} not found - run: sh ./mvnw -B -DskipTests package")
        return 1
    work = Path(tempfile.mkdtemp(prefix="crash-test-"))
    service = nc.Service(work / "ledger.db", work / "service.log")
    rows = []
    try:
        service.start()
        setup = nc.Client(service)
        pairs = []
        for i in range(4):
            src, dst = nc.make_wallet(setup, f"crash-src-{i}"), nc.make_wallet(setup, f"crash-dst-{i}")
            nc.fund(setup, src, FUNDING)
            pairs.append((src, dst))
        setup.close()
        totals = {"acknowledged": 0, "attempted": 0, "lost": 0, "duplicates": 0, "restarts": []}
        recon_ok = True
        for cycle in range(1, a.crashes + 1):
            acknowledged, attempted = load_until_killed(service, pairs, a.seconds)
            totals["acknowledged"] += len(acknowledged)
            totals["attempted"] += len(attempted)
            totals["restarts"].append(service.start())
            c = nc.Client(service)
            s, _, report, _ = c.call("GET", "/api/v1/admin/reconciliation", admin=True)
            recon_ok &= s == 200 and report.get("status") == "PASSED"
            for key, txn in acknowledged.items():
                s, _, _, _ = c.call("GET", f"/api/v1/transactions/{txn}")
                if s != 200:
                    totals["lost"] += 1
            for key, (src, dst) in attempted.items():
                s, headers, body, _ = c.call("POST", "/api/v1/transfers", body=nc.transfer_body(src, dst, 1), key=key)
                if s != 201 or (key in acknowledged and body.get("transaction_id") != acknowledged[key]):
                    totals["duplicates"] += 1
            s, _, report, _ = c.call("GET", "/api/v1/admin/reconciliation", admin=True)
            recon_ok &= s == 200 and report.get("status") == "PASSED"
            committed = report.get("transactions_checked", 0) - len(pairs)
            c.close()
            print(f"cycle {cycle}: {len(acknowledged)} acknowledged of {len(attempted)} attempted before the kill; "
                  f"after recovery and retries {committed} transfers committed in total; reconciliation "
                  f"{report.get('status')}")
        expected = totals["attempted"]
        rows.append(("NFR-SAFE-3", "all or nothing after a crash: reconciliation PASSED after every restart",
                     f"{a.crashes} hard kills under load of {WRITERS} writers", recon_ok))
        rows.append(("NFR-SAFE-4", "every 201 before the kill survives",
                     f"{totals['acknowledged']} acknowledged, {totals['lost']} lost", totals["lost"] == 0))
        rows.append(("NFR-REL-2", "a restart suffices", f"restart to /health in "
                     f"{', '.join(f'{r:.1f}' for r in totals['restarts'])} s", True))
        rows.append(("FR-2.8 after a crash", "each in-flight key posts exactly once when retried",
                     f"{expected} keys retried, {committed} transfers in the ledger, {totals['duplicates']} mismatches",
                     totals["duplicates"] == 0 and committed == expected))
    except (RuntimeError, OSError) as e:
        rows.append(("crash test", "runs to the end", f"error: {e}", False))
    finally:
        service.stop()
    table = nc.markdown_table(rows)
    print(table)
    overall = len(rows) == 4 and all(r[3] for r in rows)
    print("PASS: the ledger survived every crash" if overall else "FAIL: see the table")
    if a.out:
        stamp = dt.datetime.now().strftime("%Y-%m-%d %H:%M")
        Path(a.out).write_text(
            "# Crash test results (NFR-SAFE-3, NFR-SAFE-4, NFR-REL-2)\n\n"
            f"Measured with `python3 scripts/nfr/crash_test.py` on {stamp}.\n\n"
            f"Machine: {nc.machine()}.\n\n"
            f"{WRITERS} clients send transfers of 1 continuously; after {a.seconds:.0f} s the service process is killed "
            "hard (no shutdown), then restarted on the same database file. After each restart every acknowledged "
            "transaction is read back, every key that was in flight is retried with the same body, and "
            "reconciliation is run.\n\n" + table + "\n\n" + ("Overall: PASS\n" if overall else "Overall: FAIL\n"),
            encoding="utf-8")
        print(f"results written to {a.out}")
    return 0 if overall else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv))
