"""Tests for the simulate-transfer skill script (SRS SK-5, UI-4, TQ-7, TQ-8).

A fake API in memory stands in for the service, so each kind of defect the skill must catch can be produced on
purpose: a retried transfer posted twice, two concurrent transfers both let through, a failing reconciliation.
The real end-to-end run is the skill's evaluation (and a CI step). Run: python3 -m unittest tests.test_simulate_transfer -v
"""
import subprocess
import sys
import threading
import time
import unittest
import uuid
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]
SCRIPT = REPO / "skills" / "simulate-transfer" / "scripts" / "simulate_transfer.py"
sys.path.insert(0, str(SCRIPT.parent))
import simulate_transfer as st  # noqa: E402


class FakeApi:
    """Just enough of the ledger API for the five steps; flags switch on defects."""

    def __init__(self, replay=True, atomic=True, reconciliation="PASSED", fail_users=False):
        self.replay, self.atomic, self.reconciliation, self.fail_users = replay, atomic, reconciliation, fail_users
        self.balances, self.keys, self.lock, self.posted = {}, {}, threading.Lock(), 0

    def call(self, method, path, role, body=None, idempotency_key=None):
        if path == "/api/v1/users":
            return (500, {}, {"code": "INTERNAL_ERROR"}) if self.fail_users else (201, {}, {"user_id": str(uuid.uuid4())})
        if path.endswith("/wallets") and method == "POST":
            wallet = str(uuid.uuid4())
            self.balances[wallet] = 0
            return 201, {}, {"wallet_id": wallet}
        if path.endswith("/deposits"):
            self.balances[path.split("/")[4]] += body["amount"]
            return 201, {}, {"transaction_id": str(uuid.uuid4())}
        if path.startswith("/api/v1/wallets/"):
            return 200, {}, {"balance": self.balances[path.split("/")[4]]}
        if path == "/api/v1/transfers":
            return self.transfer(body, idempotency_key)
        if path == "/api/v1/admin/reconciliation":
            return 200, {}, {"status": self.reconciliation, "transactions_checked": self.posted,
                             "entries_checked": 2 * self.posted, "discrepancies": []}
        raise AssertionError(f"unexpected call {method} {path}")

    def transfer(self, body, key):
        if self.replay and key in self.keys:
            return 201, {"Idempotent-Replayed": "true"}, self.keys[key]
        src, dst, amount = body["source_wallet_id"], body["destination_wallet_id"], body["amount"]
        if self.atomic:
            self.lock.acquire()
        try:
            enough = self.balances[src] >= amount
            time.sleep(0.05)  # widens the race window when not atomic
            if not enough:
                return 422, {}, {"code": "INSUFFICIENT_FUNDS"}
            self.balances[src] -= amount
            self.balances[dst] += amount
            self.posted += 1
            response = {"transaction_id": str(uuid.uuid4()), "amount": amount}
            self.keys[key] = response
            return 201, {}, response
        finally:
            if self.atomic:
                self.lock.release()


def run(api):
    sim = st.Simulation(api)
    sim.run()
    return sim, {name: ok for name, ok, _ in sim.results}


class SimulationTest(unittest.TestCase):
    def test_correct_service_passes_all_five_steps(self):
        sim, marks = run(FakeApi())
        self.assertEqual(marks, {"setup": True, "initial": True, "idempotent": True, "concurrent": True,
                                 "reconcile": True})
        self.assertEqual(sim.initial, {"source": 100_000, "destination": 0})
        self.assertEqual(sim.final, {"source": 25_000, "destination": 75_000})
        text = st.report(sim, "fake")
        self.assertIn("PASS: 5/5 steps passed", text)
        self.assertIn("reconciliation: PASSED", text)
        self.assertIn("100000", text)

    def test_a_retry_that_posts_twice_fails_the_idempotent_step(self):
        sim, marks = run(FakeApi(replay=False))
        self.assertFalse(marks["idempotent"])
        self.assertIsNone(marks["concurrent"], "later steps are SKIP, never PASS")
        self.assertIn("reconcile", marks, "reconciliation always runs")
        self.assertIn("FAIL", st.report(sim, "fake"))

    def test_a_race_that_lets_both_transfers_through_fails_the_concurrent_step(self):
        _, marks = run(FakeApi(atomic=False))
        self.assertTrue(marks["idempotent"])
        self.assertFalse(marks["concurrent"])

    def test_a_failed_reconciliation_fails_the_last_step(self):
        sim, marks = run(FakeApi(reconciliation="FAILED"))
        self.assertFalse(marks["reconcile"])
        self.assertIn("reconciliation: FAILED", st.report(sim, "fake"))

    def test_setup_failure_stops_the_run(self):
        sim, marks = run(FakeApi(fail_users=True))
        self.assertEqual(marks, {"setup": False})
        self.assertIn("FAIL: 0/5 steps passed", st.report(sim, "fake"))


class MainTest(unittest.TestCase):
    def test_unknown_member_is_rejected(self):
        r = subprocess.run([sys.executable, str(SCRIPT), "--by", "Someone Else"], capture_output=True, text=True)
        self.assertEqual(r.returncode, 2)
        self.assertIn("REJECTED", r.stderr)

    def test_base_url_needs_keys_from_the_environment(self):
        env = {k: v for k, v in __import__("os").environ.items() if not k.startswith("LEDGER_")}
        r = subprocess.run([sys.executable, str(SCRIPT), "--base-url", "http://127.0.0.1:9"], capture_output=True,
                           text=True, env=env)
        self.assertEqual(r.returncode, 2)
        self.assertIn("LEDGER_CLIENT_KEYS", r.stderr)

    def test_missing_jar_fails_with_a_hint(self):
        r = subprocess.run([sys.executable, str(SCRIPT), "--jar", "/nonexistent/app.jar"], capture_output=True,
                           text=True)
        self.assertEqual(r.returncode, 1)
        self.assertIn("not found", r.stdout)
        self.assertIn("mvnw", r.stdout)


if __name__ == "__main__":
    unittest.main()
