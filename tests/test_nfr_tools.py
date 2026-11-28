"""Tests of the helpers behind the NFR scripts in scripts/nfr/ (SRS NFR-PERF-1-5, NFR-SAFE-3/4, TQ-8).

The full scripts start the service and are run by hand (results in docs/testing/); these tests pin the parts whose
mistakes would make a measurement wrong: the percentile, the FR-4.4 hash serialisation and the seeding of 100 000
entries. Run: python3 -m unittest tests.test_nfr_tools -v
"""
import sqlite3
import sys
import tempfile
import unittest
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(REPO / "scripts" / "nfr"))
import load_test  # noqa: E402
import nfr_common as nc  # noqa: E402

V1 = REPO / "src" / "main" / "resources" / "db" / "migration" / "V1__schema.sql"
U = "11111111-1111-4111-8111-111111111111"
U2 = "44444444-4444-4444-8444-444444444444"
A = "22222222-2222-4222-8222-222222222222"
B = "33333333-3333-4333-8333-333333333333"


class PercentileTest(unittest.TestCase):
    def test_nearest_rank(self):
        values = list(range(1, 101))
        self.assertEqual(nc.percentile(values, 95), 95)
        self.assertEqual(nc.percentile(values, 50), 50)
        self.assertEqual(nc.percentile([7], 95), 7)
        self.assertEqual(nc.percentile([5, 1, 3], 100), 5)


class HashChainTest(unittest.TestCase):
    def test_python_serialisation_matches_the_java_test_vector(self):
        # the same vector as HashChainTest.java: both implementations of FR-4.4 must agree
        h = nc.entry_hash("0" * 64, 1, "11111111-1111-4111-8111-111111111111",
                          "22222222-2222-4222-8222-222222222222", "33333333-3333-4333-8333-333333333333",
                          "DEBIT", 12550, "INR", "2026-11-23T10:15:30.123Z")
        self.assertEqual(h, "faf4baf1ab1d828a3606404f852afbfe4160c8a68db4f4758f256de05a14cec2")

    def test_timestamps_are_rfc3339_utc_milliseconds(self):
        self.assertRegex(nc.rfc3339_now(), r"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z$")


class SeedTest(unittest.TestCase):
    def setUp(self):
        self.db = Path(tempfile.mkdtemp()) / "seed.db"
        c = sqlite3.connect(self.db)
        c.executescript(V1.read_text(encoding="utf-8"))
        c.execute("INSERT INTO users VALUES (?, 'u', 'u@example.com', 0, 't')", (U,))
        c.execute("INSERT INTO wallets VALUES (?, ?, 'INR', 1000, 0, 't')", (A, U))
        c.execute("INSERT INTO users VALUES (?, 'v', 'v@example.com', 0, 't')", (U2,))
        c.execute("INSERT INTO wallets VALUES (?, ?, 'INR', 1000, 0, 't')", (B, U2))
        c.commit()
        c.close()

    def test_seeded_ledger_is_balanced_chained_and_leaves_balances_unchanged(self):
        n = load_test.seed_entries(self.db, A, B, 1000)
        self.assertGreaterEqual(n, 1000)
        c = sqlite3.connect(self.db)
        rows = c.execute("SELECT entry_id, sequence, transaction_id, wallet_id, direction, amount, currency, "
                         "created_at, prev_hash, entry_hash FROM ledger_entries ORDER BY sequence").fetchall()
        prev, expected_seq = nc.GENESIS, 1
        for eid, seq, txn, wallet, direction, amount, cur, at, prev_hash, entry_hash in rows:
            self.assertEqual(seq, expected_seq)
            self.assertEqual(prev_hash, prev)
            self.assertEqual(entry_hash, nc.entry_hash(prev_hash, seq, eid, txn, wallet, direction, amount, cur, at))
            prev, expected_seq = entry_hash, seq + 1
        debits = c.execute("SELECT SUM(amount) FROM ledger_entries WHERE direction = 'DEBIT'").fetchone()[0]
        credits = c.execute("SELECT SUM(amount) FROM ledger_entries WHERE direction = 'CREDIT'").fetchone()[0]
        self.assertEqual(debits, credits)
        for wallet in (A, B):
            balance = c.execute("SELECT COALESCE(SUM(CASE direction WHEN 'CREDIT' THEN amount ELSE -amount END), 0) "
                                "FROM ledger_entries WHERE wallet_id = ?", (wallet,)).fetchone()[0]
            self.assertEqual(balance, 0, "alternating transfers leave each wallet where it started")
        c.close()

    def test_seeding_continues_an_existing_chain(self):
        first = load_test.seed_entries(self.db, A, B, 10)
        second = load_test.seed_entries(self.db, A, B, first + 10)
        self.assertGreater(second, first)
        c = sqlite3.connect(self.db)
        seqs = [r[0] for r in c.execute("SELECT sequence FROM ledger_entries ORDER BY sequence")]
        self.assertEqual(seqs, list(range(1, len(seqs) + 1)))
        c.close()


class ReportTest(unittest.TestCase):
    def test_markdown_table(self):
        table = nc.markdown_table([("NFR-PERF-2", ">= 50/s", "120/s", True), ("NFR-PERF-4", "<= 10 s", "12 s", False)])
        self.assertIn("| NFR-PERF-2 | >= 50/s | 120/s | PASS |", table)
        self.assertIn("| NFR-PERF-4 | <= 10 s | 12 s | FAIL |", table)


if __name__ == "__main__":
    unittest.main()
