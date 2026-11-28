"""Shared helpers of the non-functional-requirement scripts (load_test.py, crash_test.py).

Both start their own copy of the service from target/financial-ledger-api.jar on a free local port, with a database
in a temporary folder and throwaway random API keys that are never printed (NFR-SAFE-5, SEC-5). Standard library only.
"""
from __future__ import annotations

import datetime as dt
import hashlib
import http.client
import json
import math
import os
import platform
import secrets
import socket
import subprocess
import time
import urllib.error
import urllib.request
import uuid
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
JAR = REPO / "target" / "financial-ledger-api.jar"
GENESIS = "0" * 64


def percentile(values: list[float], p: float) -> float:
    """Nearest-rank percentile (p in 0..100) of a non-empty list."""
    ordered = sorted(values)
    rank = max(1, math.ceil(p / 100 * len(ordered)))
    return ordered[rank - 1]


def entry_hash(prev_hash: str, sequence: int, entry_id: str, transaction_id: str, wallet_id: str, direction: str,
               amount: int, currency: str, created_at: str) -> str:
    """FR-4.4 / ADR-0003 serialisation, written independently of the Java code (so a PASSED reconciliation over
    rows made here also cross-checks the Java implementation)."""
    text = (f"{prev_hash}\n{sequence}|{entry_id}|{transaction_id}|{wallet_id}|{direction}|{amount}|{currency}|"
            f"{created_at}")
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def rfc3339_now() -> str:
    now = dt.datetime.now(dt.timezone.utc)
    return now.strftime("%Y-%m-%dT%H:%M:%S.") + f"{now.microsecond // 1000:03d}Z"


def free_port() -> int:
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.bind(("127.0.0.1", 0))
        return s.getsockname()[1]


class Service:
    """One running copy of the service; start() returns the seconds until GET /health answered 200."""

    def __init__(self, db_path: Path, log_path: Path, jar: Path = JAR):
        self.db_path, self.log_path, self.jar = db_path, log_path, jar
        self.port = free_port()
        self.client_key, self.admin_key = secrets.token_urlsafe(32), secrets.token_urlsafe(32)
        self.proc: subprocess.Popen | None = None

    @property
    def base(self) -> str:
        return f"http://127.0.0.1:{self.port}"

    def start(self, timeout: float = 90.0) -> float:
        env = dict(os.environ, LEDGER_HOST="127.0.0.1", LEDGER_PORT=str(self.port), LEDGER_DB_PATH=str(self.db_path),
                   LEDGER_CLIENT_KEYS=self.client_key, LEDGER_ADMIN_KEYS=self.admin_key)
        log = self.log_path.open("a", encoding="utf-8")
        t0 = time.monotonic()
        self.proc = subprocess.Popen(["java", "-jar", str(self.jar)], cwd=REPO, env=env, stdout=log,
                                     stderr=subprocess.STDOUT)
        while time.monotonic() - t0 < timeout:
            if self.proc.poll() is not None:
                raise RuntimeError(f"service exited with code {self.proc.returncode}; see {self.log_path}")
            try:
                with urllib.request.urlopen(self.base + "/health", timeout=2) as r:
                    if r.status == 200:
                        return time.monotonic() - t0
            except (urllib.error.URLError, OSError):
                time.sleep(0.05)
        self.kill()
        raise RuntimeError(f"service not ready within {timeout:.0f} s; see {self.log_path}")

    def stop(self) -> None:
        if self.proc and self.proc.poll() is None:
            self.proc.terminate()
            try:
                self.proc.wait(timeout=20)
            except subprocess.TimeoutExpired:
                self.kill()

    def kill(self) -> None:
        """Hard kill (SIGKILL on Linux/macOS, TerminateProcess on Windows) - simulates a crash."""
        if self.proc and self.proc.poll() is None:
            self.proc.kill()
            self.proc.wait()


class Client:
    """A keep-alive HTTP/1.1 connection; call() returns (status, headers, json, seconds)."""

    def __init__(self, service: Service, timeout: float = 30.0):
        self.service = service
        self.conn = http.client.HTTPConnection("127.0.0.1", service.port, timeout=timeout)

    def call(self, method: str, path: str, admin: bool = False, body: dict | None = None,
             key: str | None = None) -> tuple[int, dict, dict, float]:
        headers = {"X-API-Key": self.service.admin_key if admin else self.service.client_key}
        data = None
        if body is not None:
            data = json.dumps(body)
            headers["Content-Type"] = "application/json"
        if key:
            headers["Idempotency-Key"] = key
        t0 = time.perf_counter()
        try:
            self.conn.request(method, path, body=data, headers=headers)
            resp = self.conn.getresponse()
            raw = resp.read()
        except (http.client.HTTPException, OSError):
            self.conn.close()
            raise
        elapsed = time.perf_counter() - t0
        try:
            payload = json.loads(raw) if raw else {}
        except ValueError:
            payload = {}
        return resp.status, dict(resp.getheaders()), payload, elapsed

    def close(self) -> None:
        self.conn.close()


def new_key() -> str:
    return str(uuid.uuid4())


def make_wallet(c: Client, label: str, currency: str = "INR") -> str:
    s, _, u, _ = c.call("POST", "/api/v1/users", body={"name": label, "email": f"{label}-{uuid.uuid4().hex}@example.com"})
    if s != 201:
        raise RuntimeError(f"create user answered {s}")
    s, _, w, _ = c.call("POST", f"/api/v1/users/{u['user_id']}/wallets", body={"currency": currency})
    if s != 201:
        raise RuntimeError(f"create wallet answered {s}")
    return w["wallet_id"]


def fund(c: Client, wallet_id: str, amount: int, currency: str = "INR") -> None:
    s, _, _, _ = c.call("POST", f"/api/v1/wallets/{wallet_id}/deposits", admin=True,
                        body={"amount": amount, "currency": currency}, key=new_key())
    if s != 201:
        raise RuntimeError(f"deposit answered {s}")


def transfer_body(source: str, destination: str, amount: int, currency: str = "INR") -> dict:
    return {"source_wallet_id": source, "destination_wallet_id": destination, "amount": amount, "currency": currency}


def machine() -> str:
    java = subprocess.run(["java", "-version"], capture_output=True, text=True).stderr.splitlines()
    return (f"{platform.system()} {platform.release()} ({platform.machine()}), {os.cpu_count()} CPUs, "
            f"Python {platform.python_version()}, {java[0] if java else 'java ?'}")


def markdown_table(rows: list[tuple[str, str, str, bool]]) -> str:
    out = ["| Requirement | Target | Measured | Result |", "|---|---|---|---|"]
    for req, target, measured, ok in rows:
        out.append(f"| {req} | {target} | {measured} | {'PASS' if ok else 'FAIL'} |")
    return "\n".join(out)
