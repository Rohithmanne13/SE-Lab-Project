#!/usr/bin/env python3
"""simulate-transfer: run a complete product workflow and verify ledger consistency (SRS SK-5).

Steps, in order (SK-5, project statement section 3):
  1. setup        - create two users with INR wallets; fund the source with an ADMIN deposit
  2. initial      - record both balances through GET /wallets/{walletId}
  3. idempotent   - transfer with an Idempotency-Key, resend it with the same key: the resend must be a replay
                    (Idempotent-Replayed: true, identical body) and the balances must change only once
  4. concurrent   - fire two transfers from the source at the same instant whose total exceeds its balance:
                    exactly one must succeed (201) and the other fail with 422 INSUFFICIENT_FUNDS
  5. reconcile    - GET /admin/reconciliation must report PASSED; then a summary of initial and final balances,
                    each step's PASS/FAIL and the reconciliation status (UI-4)

Without --base-url the script starts target/financial-ledger-api.jar itself, on a free local port, with a throwaway
database and throwaway random API keys that are never printed (NFR-SAFE-5, SEC-5). With --base-url it uses a running
service and reads the keys from LEDGER_CLIENT_KEYS and LEDGER_ADMIN_KEYS (the first key of each).

Usage (from the repository root):
  python3 skills/simulate-transfer/scripts/simulate_transfer.py [--by "Name"] [--base-url URL] [--jar PATH]
Exit status 0 only when every step passed and reconciliation is PASSED; 2 for a bad argument.
Standard library only; works on Windows, macOS and Linux (OE-1).
"""
from __future__ import annotations

import argparse
import datetime as dt
import json
import os
import platform
import secrets
import socket
import subprocess
import sys
import tempfile
import threading
import time
import urllib.error
import urllib.request
import uuid
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
JAR = REPO / "target" / "financial-ledger-api.jar"
TEAM_MEMBERS = ("Manne Rohith Sai", "Sunnapu Suhith Reddy", "Rudraraju Dhanush Varma", "Abhiram P B S")

FUNDING = 100_000        # INR 1000.00 deposited into the source wallet
TRANSFER = 25_000        # INR 250.00, sent twice with the same key (step 3)
CONCURRENT = 50_000      # INR 500.00 each, two at once (step 4): 100 000 > 75 000 left
CURRENCY = "INR"


class Api:
    """Minimal JSON client of the ledger API; every call returns (status, headers, json body)."""

    def __init__(self, base_url: str, client_key: str, admin_key: str, timeout: float = 30.0):
        self.base = base_url.rstrip("/")
        self.keys = {"CLIENT": client_key, "ADMIN": admin_key}
        self.timeout = timeout

    def call(self, method: str, path: str, role: str, body: dict | None = None,
             idempotency_key: str | None = None) -> tuple[int, dict, dict]:
        headers = {"X-API-Key": self.keys[role], "Accept": "application/json"}
        data = None
        if body is not None:
            data = json.dumps(body).encode("utf-8")
            headers["Content-Type"] = "application/json"
        if idempotency_key:
            headers["Idempotency-Key"] = idempotency_key
        req = urllib.request.Request(self.base + path, data=data, method=method, headers=headers)
        try:
            with urllib.request.urlopen(req, timeout=self.timeout) as resp:
                return resp.status, dict(resp.headers), _json(resp.read())
        except urllib.error.HTTPError as e:
            return e.code, dict(e.headers), _json(e.read())


def _json(raw: bytes) -> dict:
    try:
        value = json.loads(raw.decode("utf-8")) if raw else {}
        return value if isinstance(value, dict) else {"value": value}
    except ValueError:
        return {"raw": raw.decode("utf-8", "replace")}


def new_key() -> str:
    return str(uuid.uuid4())


class Simulation:
    """The five SK-5 steps against one API; results are (step, ok or None for SKIP, detail)."""

    def __init__(self, api: Api):
        self.api = api
        self.results: list[tuple[str, bool | None, str]] = []
        self.source = self.destination = None
        self.initial: dict[str, int] = {}
        self.final: dict[str, int] = {}
        self.reconciliation = "not run"

    def balance(self, wallet_id: str) -> int:
        status, _, body = self.api.call("GET", f"/api/v1/wallets/{wallet_id}", "CLIENT")
        if status != 200 or not isinstance(body.get("balance"), int):
            raise RuntimeError(f"GET wallet answered {status}")
        return body["balance"]

    def transfer(self, amount: int, key: str) -> tuple[int, dict, dict]:
        return self.api.call("POST", "/api/v1/transfers", "CLIENT", {
            "source_wallet_id": self.source, "destination_wallet_id": self.destination,
            "amount": amount, "currency": CURRENCY, "description": "simulate-transfer"}, key)

    def step_setup(self) -> tuple[bool, str]:
        run = uuid.uuid4().hex[:12]
        wallets = []
        for who in ("source", "destination"):
            s, _, u = self.api.call("POST", "/api/v1/users", "CLIENT",
                                    {"name": f"Simulate {who}", "email": f"simulate-{who}-{run}@example.com"})
            if s != 201:
                return False, f"create {who} user answered {s} {u.get('code', '')}"
            s, _, w = self.api.call("POST", f"/api/v1/users/{u['user_id']}/wallets", "CLIENT", {"currency": CURRENCY})
            if s != 201:
                return False, f"create {who} wallet answered {s} {w.get('code', '')}"
            wallets.append(w["wallet_id"])
        self.source, self.destination = wallets
        s, _, d = self.api.call("POST", f"/api/v1/wallets/{self.source}/deposits", "ADMIN",
                                {"amount": FUNDING, "currency": CURRENCY, "description": "simulate-transfer funding"},
                                new_key())
        if s != 201:
            return False, f"deposit answered {s} {d.get('code', '')}"
        return True, f"2 users, 2 {CURRENCY} wallets, deposit {FUNDING} into the source"

    def step_initial(self) -> tuple[bool, str]:
        self.initial = {"source": self.balance(self.source), "destination": self.balance(self.destination)}
        ok = self.initial == {"source": FUNDING, "destination": 0}
        return ok, f"source {self.initial['source']}, destination {self.initial['destination']}"

    def step_idempotent(self) -> tuple[bool, str]:
        key = new_key()
        s1, _, b1 = self.transfer(TRANSFER, key)
        s2, h2, b2 = self.transfer(TRANSFER, key)
        replayed = {k.lower(): v for k, v in h2.items()}.get("idempotent-replayed") == "true"
        src, dst = self.balance(self.source), self.balance(self.destination)
        once = (src, dst) == (FUNDING - TRANSFER, TRANSFER)
        ok = s1 == 201 and s2 == 201 and replayed and b1 == b2 and once
        return ok, (f"first {s1}, resend {s2} replayed={'yes' if replayed else 'no'} same body={'yes' if b1 == b2 else 'no'};"
                    f" balances {src}/{dst} changed {'once' if once else 'WRONG'}")

    def step_concurrent(self) -> tuple[bool, str]:
        before = self.balance(self.source)
        barrier = threading.Barrier(2)
        outcomes: list[tuple[int, str]] = []
        lock = threading.Lock()

        def fire():
            barrier.wait()
            s, _, b = self.transfer(CONCURRENT, new_key())
            with lock:
                outcomes.append((s, b.get("code", "")))

        threads = [threading.Thread(target=fire) for _ in range(2)]
        for t in threads:
            t.start()
        for t in threads:
            t.join(timeout=60)
        statuses = sorted(s for s, _ in outcomes)
        rejected = [c for s, c in outcomes if s == 422]
        src = self.balance(self.source)
        ok = statuses == [201, 422] and rejected == ["INSUFFICIENT_FUNDS"] and src == before - CONCURRENT
        return ok, (f"2 x {CONCURRENT} from {before}: {statuses.count(201)} succeeded, "
                    f"{len(rejected)} INSUFFICIENT_FUNDS; source now {src}")

    def step_reconcile(self) -> tuple[bool, str]:
        s, _, report = self.api.call("GET", "/api/v1/admin/reconciliation", "ADMIN")
        self.final = {"source": self.balance(self.source), "destination": self.balance(self.destination)}
        self.reconciliation = report.get("status", f"HTTP {s}")
        ok = s == 200 and self.reconciliation == "PASSED"
        return ok, (f"{self.reconciliation}: {report.get('transactions_checked')} transactions, "
                    f"{report.get('entries_checked')} entries, {len(report.get('discrepancies', []))} discrepancies")

    def run(self) -> list[tuple[str, bool | None, str]]:
        steps = [("setup", self.step_setup), ("initial", self.step_initial), ("idempotent", self.step_idempotent),
                 ("concurrent", self.step_concurrent), ("reconcile", self.step_reconcile)]
        failed = False
        for name, step in steps:
            if failed and name != "reconcile":
                self.results.append((name, None, "skipped - an earlier step failed"))
                continue
            try:
                ok, detail = step()
            except (RuntimeError, OSError, KeyError, TypeError) as e:
                ok, detail = False, f"error: {e}"
            self.results.append((name, ok, detail))
            failed = failed or not ok
            if name == "setup" and not ok:
                break
        return self.results


def report(sim: Simulation, target: str) -> str:
    lines = ["simulate-transfer report", f"target   {target}"]
    for name, ok, detail in sim.results:
        mark = "SKIP" if ok is None else ("PASS" if ok else "FAIL")
        lines.append(f"{mark}  {name:<10} {detail}")
    if sim.initial or sim.final:
        lines.append("balances (minor units)   initial   final")
        for side in ("source", "destination"):
            lines.append(f"  {side:<22} {sim.initial.get(side, '-')!s:>8}  {sim.final.get(side, '-')!s:>6}")
    passed = sum(1 for _, ok, _ in sim.results if ok)
    overall = len(sim.results) == 5 and all(ok for _, ok, _ in sim.results)
    lines.append(f"reconciliation: {sim.reconciliation}")
    lines.append(f"{'PASS' if overall else 'FAIL'}: {passed}/5 steps passed")
    return "\n".join(lines)


def log_run(by: str, sim: Simulation, target: str) -> None:
    passed = sum(1 for _, ok, _ in sim.results if ok)
    status = "PASS" if passed == 5 else "FAIL"
    stamp = dt.datetime.now().strftime("%Y-%m-%d %H:%M")
    line = (f"- {stamp} | simulate-transfer | {status} {passed}/5 steps, reconciliation {sim.reconciliation} | {by} | "
            f"{target}, {platform.system()} {platform.release()}\n")
    with (REPO / "brain" / "log.md").open("a", encoding="utf-8") as fh:
        fh.write(line)


def free_port() -> int:
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.bind(("127.0.0.1", 0))
        return s.getsockname()[1]


def start_service(jar: Path, work: Path, timeout: float) -> tuple[subprocess.Popen, str, str, str]:
    """Starts the jar with a temporary database and fresh random keys; returns (process, url, client, admin)."""
    port = free_port()
    client, admin = secrets.token_urlsafe(32), secrets.token_urlsafe(32)
    env = dict(os.environ, LEDGER_HOST="127.0.0.1", LEDGER_PORT=str(port), LEDGER_DB_PATH=str(work / "simulate.db"),
               LEDGER_CLIENT_KEYS=client, LEDGER_ADMIN_KEYS=admin)
    log = (work / "service.log").open("w", encoding="utf-8")
    proc = subprocess.Popen(["java", "-jar", str(jar)], cwd=REPO, env=env, stdout=log, stderr=subprocess.STDOUT)
    url = f"http://127.0.0.1:{port}"
    start = time.monotonic()
    while time.monotonic() - start < timeout:
        if proc.poll() is not None:
            raise RuntimeError(f"the service exited with code {proc.returncode}; see {work / 'service.log'}")
        try:
            with urllib.request.urlopen(url + "/health", timeout=2):
                return proc, url, client, admin
        except (urllib.error.URLError, OSError):
            time.sleep(0.5)
    proc.terminate()
    raise RuntimeError(f"the service did not answer within {timeout:.0f} s; see {work / 'service.log'}")


def stop(proc: subprocess.Popen | None) -> None:
    if proc and proc.poll() is None:
        proc.terminate()
        try:
            proc.wait(timeout=15)
        except subprocess.TimeoutExpired:
            proc.kill()
            proc.wait()


def main(argv: list[str]) -> int:
    ap = argparse.ArgumentParser(description="Run the SK-5 workflow and verify ledger consistency.")
    ap.add_argument("--by", default="", help="Team 007 member running the skill; logs the run in brain/log.md")
    ap.add_argument("--base-url", default="", help="use a running service (keys from LEDGER_CLIENT_KEYS/ADMIN_KEYS)")
    ap.add_argument("--jar", default=str(JAR), help="jar to start when no --base-url is given")
    ap.add_argument("--timeout", type=float, default=60.0, help="seconds to wait for the service to start")
    a = ap.parse_args(argv[1:])
    if a.by and a.by not in TEAM_MEMBERS:
        print(f"simulate-transfer: REJECTED - --by must be one of {TEAM_MEMBERS}", file=sys.stderr)
        return 2
    proc = None
    try:
        if a.base_url:
            client = os.environ.get("LEDGER_CLIENT_KEYS", "").split(",")[0].strip()
            admin = os.environ.get("LEDGER_ADMIN_KEYS", "").split(",")[0].strip()
            if not client or not admin:
                print("simulate-transfer: REJECTED - set LEDGER_CLIENT_KEYS and LEDGER_ADMIN_KEYS for --base-url",
                      file=sys.stderr)
                return 2
            url, target = a.base_url, a.base_url
        else:
            jar = Path(a.jar)
            if not jar.is_file():
                print(f"simulate-transfer: FAIL - {jar} not found; build it first: sh ./mvnw -B -DskipTests package")
                return 1
            work = Path(tempfile.mkdtemp(prefix="simulate-transfer-"))
            try:
                proc, url, client, admin = start_service(jar, work, a.timeout)
            except RuntimeError as e:
                print(f"simulate-transfer: FAIL - {e}")
                return 1
            target = "local jar, throwaway database"
        sim = Simulation(Api(url, client, admin))
        sim.run()
    finally:
        stop(proc)
    print(report(sim, target))
    if a.by:
        log_run(a.by, sim, target)
    ok = len(sim.results) == 5 and all(r[1] for r in sim.results)
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv))
