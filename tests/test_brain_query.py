"""Tests for the brain-query skill script (SRS SK-3, TQ-8).

Run: python3 -m unittest tests.test_brain_query -v
"""
import shutil
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]
QUERY = REPO / "skills" / "brain-query" / "scripts" / "query.py"
sys.path.insert(0, str(QUERY.parent))
import query  # noqa: E402

BY = "Rudraraju Dhanush Varma"


class QueryTest(unittest.TestCase):
    def setUp(self):
        self.tmp = Path(tempfile.mkdtemp())
        self.vault = self.tmp / "brain"
        shutil.copytree(REPO / "brain", self.vault, ignore=shutil.ignore_patterns(".obsidian"))

    def tearDown(self):
        shutil.rmtree(self.tmp)

    def run_query(self, *args):
        return subprocess.run([sys.executable, str(QUERY), "--vault", str(self.vault)] + list(args),
                              capture_output=True, text=True)

    def test_search_ranks_the_matching_note_first(self):
        names = [r["name"] for r in query.search(self.vault, "write-ahead logging readers writer checkpoint")]
        self.assertEqual(names[0], "sqlite-write-ahead-logging")

    def test_search_never_returns_index_or_log(self):
        names = [r["name"] for r in query.search(self.vault, "brain skill run log index inbox", top=50)]
        self.assertNotIn("index", names)
        self.assertNotIn("log", names)

    def test_search_without_matches_says_so(self):
        r = self.run_query("search", "zzqx unrelatedword")
        self.assertEqual(r.returncode, 0)
        self.assertIn("the vault does not contain an answer", r.stdout)

    def test_log_records_cited_notes(self):
        r = self.run_query("log", "Which status for a reused key?", "--by", BY, "--cited", "idempotency-key-header-draft")
        self.assertEqual(r.returncode, 0, r.stderr)
        log = (self.vault / "log.md").read_text(encoding="utf-8")
        self.assertIn(f'| brain-query | answered "Which status for a reused key?" | {BY} | cited [[idempotency-key-header-draft]]', log)

    def test_log_no_answer(self):
        r = self.run_query("log", "Capital of Mars?", "--by", BY, "--no-answer")
        self.assertEqual(r.returncode, 0, r.stderr)
        self.assertIn("the vault does not contain the answer", (self.vault / "log.md").read_text(encoding="utf-8"))

    def test_log_refusals_write_nothing(self):
        before = (self.vault / "log.md").read_bytes()
        for args in (["log", "q", "--by", "Someone Else", "--cited", "para-method"],
                     ["log", "q", "--by", BY, "--cited", "no-such-note"],
                     ["log", " ", "--by", BY, "--no-answer"]):
            with self.subTest(args=args):
                r = self.run_query(*args)
                self.assertEqual(r.returncode, 1)
                self.assertIn("REJECTED", r.stderr)
        self.assertEqual((self.vault / "log.md").read_bytes(), before)

    def test_evaluation_set_passes(self):
        r = subprocess.run([sys.executable, str(QUERY.parent / "eval_query.py"), "--vault", str(self.vault)],
                           capture_output=True, text=True)
        self.assertEqual(r.returncode, 0, r.stdout)


if __name__ == "__main__":
    unittest.main()
