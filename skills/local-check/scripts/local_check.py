#!/usr/bin/env python3
"""local-check: confirm the project works on this machine (SRS SK-4, SDLC section 3).

Steps, in order:
  1. build      - Maven -B -DskipTests package (includes the Checkstyle lint)
  2. tests      - Maven -B test, then python3 -m unittest discover -s tests
  Maven is the repository's Maven Wrapper (sh ./mvnw / mvnw.cmd) unless --mvn names another command.
  3. server     - start target/financial-ledger-api.jar on a free local port, with a throwaway
                  database file (never the real one, NFR-SAFE-5) and throwaway random API keys
                  (never printed or logged, SEC-5), and wait until it answers
  4. fetch      - GET /health (read-only) must return 200 with status "ok" and database "ok"
  5. report     - PASS or FAIL per step (UI-4); the server is always stopped; exit status 1 if any
                  step failed. With --by "<member>" the run is also logged in brain/log.md (SB-0).

Usage (from the repository root):
  python3 skills/local-check/scripts/local_check.py [--by "Name"] [--mvn CMD] [--port N] [--timeout 60]
Standard library only; works on Windows, macOS and Linux (OE-1, NFR-PORT-1).
"""
from __future__ import annotations

import argparse
import datetime as dt
import json
import os
import platform
import secrets
import shutil
import socket
import subprocess
import sys
import tempfile
import time
import urllib.error
import urllib.request
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
JAR = REPO / "target" / "financial-ledger-api.jar"
TEAM_MEMBERS = ("Manne Rohith Sai", "Sunnapu Suhith Reddy", "Rudraraju Dhanush Varma", "Abhiram P B S")


def free_port() -> int:
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.bind(("127.0.0.1", 0))
        return s.getsockname()[1]


def maven_command(override: str) -> list[str] | None:
    """The Maven command to use: --mvn if given, else the Maven Wrapper, else mvn on the PATH; None if none exists."""
    if override:
        found = shutil.which(override) or (override if Path(override).exists() else None)
        return [found] if found else None
    if os.name == "nt" and (REPO / "mvnw.cmd").exists():
        return [str(REPO / "mvnw.cmd")]
    if (REPO / "mvnw").exists() and shutil.which("sh"):
        return [shutil.which("sh"), str(REPO / "mvnw")]
    found = shutil.which("mvn")
    return [found] if found else None


def server_env(port: int, db_file: Path) -> dict[str, str]:
    """Environment for the throwaway server: local port, temporary database, fresh random API keys (SEC-5)."""
    return dict(os.environ, LEDGER_HOST="127.0.0.1", LEDGER_PORT=str(port), LEDGER_DB_PATH=str(db_file),
                LEDGER_CLIENT_KEYS=secrets.token_urlsafe(32), LEDGER_ADMIN_KEYS=secrets.token_urlsafe(32))


def run(cmd: list[str], log_file: Path) -> tuple[bool, str]:
    """Run a command from the repository root; output goes to log_file. Returns (ok, summary)."""
    with log_file.open("a", encoding="utf-8") as fh:
        fh.write(f"\n$ {' '.join(cmd)}\n")
        fh.flush()
        proc = subprocess.run(cmd, cwd=REPO, stdout=fh, stderr=subprocess.STDOUT)
    return proc.returncode == 0, f"exit {proc.returncode}"


def fetch_health(url: str, timeout: float = 5.0) -> tuple[bool, str]:
    """GET the health URL; ok only for 200 with status 'ok' and database 'ok' (FR-6.4)."""
    try:
        with urllib.request.urlopen(url, timeout=timeout) as resp:
            body = json.loads(resp.read().decode("utf-8"))
            ok = resp.status == 200 and body.get("status") == "ok" and body.get("database") == "ok"
            return ok, f"HTTP {resp.status} {json.dumps(body, separators=(',', ':'))}"
    except urllib.error.HTTPError as e:
        return False, f"HTTP {e.code}"
    except (urllib.error.URLError, OSError, ValueError) as e:
        return False, f"no answer ({e.__class__.__name__})"


def wait_until_up(url: str, proc: subprocess.Popen, timeout: float) -> tuple[bool, str]:
    start = time.monotonic()
    while time.monotonic() - start < timeout:
        if proc.poll() is not None:
            return False, f"server exited with code {proc.returncode}"
        try:
            with urllib.request.urlopen(url, timeout=2):
                return True, f"answering after {time.monotonic() - start:.1f} s"
        except urllib.error.HTTPError:
            return True, f"answering after {time.monotonic() - start:.1f} s"
        except (urllib.error.URLError, OSError):
            time.sleep(0.5)
    return False, f"no answer within {timeout:.0f} s"


def stop(proc: subprocess.Popen | None) -> None:
    if proc and proc.poll() is None:
        proc.terminate()
        try:
            proc.wait(timeout=15)
        except subprocess.TimeoutExpired:
            proc.kill()
            proc.wait()


def report(results: list[tuple[str, bool | None, str]]) -> str:
    lines = ["local-check report"]
    for name, ok, detail in results:
        mark = "SKIP" if ok is None else ("PASS" if ok else "FAIL")
        lines.append(f"{mark}  {name:<8} {detail}")
    passed = sum(1 for _, ok, _ in results if ok)
    overall = all(ok for _, ok, _ in results)
    lines.append(f"{'PASS' if overall else 'FAIL'}: {passed}/{len(results)} steps passed on {platform.system()}")
    return "\n".join(lines)


def log_run(by: str, results: list[tuple[str, bool | None, str]]) -> None:
    passed = sum(1 for _, ok, _ in results if ok)
    status = "PASS" if all(ok for _, ok, _ in results) else "FAIL"
    stamp = dt.datetime.now().strftime("%Y-%m-%d %H:%M")
    line = (f"- {stamp} | local-check | {status} {passed}/{len(results)} steps | {by} | "
            f"{platform.system()} {platform.release()}, Python {platform.python_version()}\n")
    with (REPO / "brain" / "log.md").open("a", encoding="utf-8") as fh:
        fh.write(line)


def main(argv: list[str]) -> int:
    ap = argparse.ArgumentParser(description="Check that the project builds, tests, starts and answers (SK-4).")
    ap.add_argument("--by", default="", help="Team 007 member running the check; logs the run in brain/log.md")
    ap.add_argument("--mvn", default=os.environ.get("MVN", ""),
                    help="Maven command (default: the repository's Maven Wrapper, else mvn)")
    ap.add_argument("--port", type=int, default=0, help="port for the server (default: a free port)")
    ap.add_argument("--timeout", type=float, default=60.0, help="seconds to wait for the server")
    a = ap.parse_args(argv[1:])
    if a.by and a.by not in TEAM_MEMBERS:
        print(f"local-check: REJECTED - --by must be one of {TEAM_MEMBERS}", file=sys.stderr)
        return 2
    mvn = maven_command(a.mvn)
    work = Path(tempfile.mkdtemp(prefix="local-check-"))
    log_file = work / "local-check.log"
    results: list[tuple[str, bool | None, str]] = []
    proc = None
    try:
        if mvn is None:
            results.append(("build", False, f"Maven not found ('{a.mvn or 'mvnw'}') - install JDK 21; see the SKILL.md"))
        else:
            ok, detail = run(mvn + ["-B", "-q", "-DskipTests", "package"], log_file)
            results.append(("build", ok and JAR.exists(), detail if JAR.exists() else f"{detail}, {JAR.name} missing"))
        if results[-1][1]:
            ok_java, d_java = run(mvn + ["-B", "-q", "test"], log_file)
            ok_py, d_py = run([sys.executable, "-m", "unittest", "discover", "-s", "tests"], log_file)
            results.append(("tests", ok_java and ok_py, f"Maven tests {d_java}; Python tests {d_py}"))
        else:
            results.append(("tests", None, "skipped - build failed"))
        if results[0][1]:
            port = a.port or free_port()
            env = server_env(port, work / "local-check.db")
            with log_file.open("a", encoding="utf-8") as fh:
                proc = subprocess.Popen(["java", "-jar", str(JAR)], cwd=REPO, env=env,
                                        stdout=fh, stderr=subprocess.STDOUT)
            url = f"http://127.0.0.1:{port}/health"
            ok, detail = wait_until_up(url, proc, a.timeout)
            results.append(("server", ok, f"{url} {detail}"))
            if ok:
                results.append(("fetch", *fetch_health(url)))
            else:
                results.append(("fetch", None, "skipped - server not running"))
        else:
            results.append(("server", None, "skipped - build failed"))
            results.append(("fetch", None, "skipped - build failed"))
    finally:
        stop(proc)
    overall = all(ok for _, ok, _ in results)
    results.append(("report", overall, f"details in {log_file}"))
    print(report(results))
    if a.by:
        log_run(a.by, results)
    return 0 if overall else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv))
