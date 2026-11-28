"""Tests for the local-check skill script (SRS SK-4, UI-4, TQ-8).

Run: python3 -m unittest tests.test_local_check -v
They never run a full build (that would call this test suite again); the real run is the skill's evaluation.
"""
import json
import subprocess
import sys
import threading
import unittest
from http.server import BaseHTTPRequestHandler, HTTPServer
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]
SCRIPT = REPO / "skills" / "local-check" / "scripts" / "local_check.py"
sys.path.insert(0, str(SCRIPT.parent))
import local_check  # noqa: E402


class _Health(BaseHTTPRequestHandler):
    status = 200
    body = {"status": "ok", "database": "ok", "version": "test"}

    def do_GET(self):  # noqa: N802 (http.server API)
        data = json.dumps(self.body).encode()
        self.send_response(self.status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def log_message(self, *args):
        pass


class FetchHealthTest(unittest.TestCase):
    def serve(self, status, body):
        handler = type("H", (_Health,), {"status": status, "body": body})
        server = HTTPServer(("127.0.0.1", 0), handler)
        threading.Thread(target=server.serve_forever, daemon=True).start()
        self.addCleanup(server.shutdown)
        return f"http://127.0.0.1:{server.server_address[1]}/health"

    def test_ok_health_passes(self):
        ok, detail = local_check.fetch_health(self.serve(200, {"status": "ok", "database": "ok", "version": "1"}))
        self.assertTrue(ok, detail)
        self.assertIn("HTTP 200", detail)

    def test_degraded_health_fails(self):
        ok, detail = local_check.fetch_health(self.serve(503, {"status": "degraded", "database": "unavailable"}))
        self.assertFalse(ok)
        self.assertIn("HTTP 503", detail)

    def test_ok_status_with_bad_body_fails(self):
        ok, _ = local_check.fetch_health(self.serve(200, {"status": "ok", "database": "unavailable"}))
        self.assertFalse(ok)

    def test_no_server_fails(self):
        ok, detail = local_check.fetch_health(f"http://127.0.0.1:{local_check.free_port()}/health", timeout=1)
        self.assertFalse(ok)
        self.assertIn("no answer", detail)


class ReportTest(unittest.TestCase):
    def test_report_marks_each_step(self):
        text = local_check.report([("build", True, "exit 0"), ("tests", False, "exit 1"), ("server", None, "skipped")])
        self.assertIn("PASS  build", text)
        self.assertIn("FAIL  tests", text)
        self.assertIn("SKIP  server", text)
        self.assertIn("FAIL: 1/3 steps passed", text)


class ServerEnvTest(unittest.TestCase):
    def test_server_gets_a_temporary_database_and_fresh_random_keys(self):
        # Since Stage 3 the service refuses to start without API keys (SEC-5); local-check generates throwaway ones.
        a = local_check.server_env(12345, Path("/tmp/x.db"))
        b = local_check.server_env(12345, Path("/tmp/x.db"))
        self.assertEqual(a["LEDGER_PORT"], "12345")
        self.assertEqual(a["LEDGER_DB_PATH"], str(Path("/tmp/x.db")))
        for name in ("LEDGER_CLIENT_KEYS", "LEDGER_ADMIN_KEYS"):
            self.assertGreaterEqual(len(a[name]), 32)
            self.assertNotEqual(a[name], b[name])
        self.assertNotEqual(a["LEDGER_CLIENT_KEYS"], a["LEDGER_ADMIN_KEYS"])


class MainTest(unittest.TestCase):
    def test_missing_maven_fails_with_non_zero_exit(self):
        r = subprocess.run([sys.executable, str(SCRIPT), "--mvn", "/nonexistent/mvn"], capture_output=True, text=True)
        self.assertEqual(r.returncode, 1, r.stdout + r.stderr)
        self.assertIn("FAIL  build", r.stdout)
        self.assertIn("SKIP  tests", r.stdout)
        self.assertIn("SKIP  fetch", r.stdout)

    def test_unknown_member_is_rejected(self):
        r = subprocess.run([sys.executable, str(SCRIPT), "--by", "Someone Else"], capture_output=True, text=True)
        self.assertEqual(r.returncode, 2)
        self.assertIn("REJECTED", r.stderr)


if __name__ == "__main__":
    unittest.main()
