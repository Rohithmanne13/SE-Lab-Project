"""Tests for the CI coverage report (SRS NFR-MAINT-2, PR-5, TQ-8).

Run: python3 -m unittest tests.test_coverage_summary -v
"""
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]
SCRIPT = REPO / "scripts" / "ci" / "coverage_summary.py"
sys.path.insert(0, str(SCRIPT.parent))
import coverage_summary as cs  # noqa: E402

HEADER = ("GROUP,PACKAGE,CLASS,INSTRUCTION_MISSED,INSTRUCTION_COVERED,BRANCH_MISSED,BRANCH_COVERED,"
          "LINE_MISSED,LINE_COVERED,COMPLEXITY_MISSED,COMPLEXITY_COVERED,METHOD_MISSED,METHOD_COVERED\n")


def csv_file(rows):
    tmp = Path(tempfile.mkdtemp()) / "jacoco.csv"
    lines = [HEADER] + [f"g,edu.iiitb.team007.ledger.{p},C{i},0,0,0,0,{missed},{covered},0,0,0,0\n"
                        for i, (p, missed, covered) in enumerate(rows)]
    tmp.write_text("".join(lines), encoding="utf-8")
    return tmp


class CoverageSummaryTest(unittest.TestCase):
    def test_sums_classes_per_package(self):
        lines = cs.package_lines(csv_file([("service", 1, 9), ("service", 1, 9), ("api", 5, 5)]))
        self.assertEqual(lines["edu.iiitb.team007.ledger.service"], (18, 20))
        self.assertEqual(lines["edu.iiitb.team007.ledger.api"], (5, 10))

    def test_gate_passes_at_eighty_percent_and_ignores_other_packages(self):
        table, failures = cs.summarise(cs.package_lines(csv_file(
            [("service", 2, 8), ("domain", 0, 5), ("persistence", 1, 9), ("api", 9, 1)])), 80.0)
        self.assertEqual(failures, [])
        self.assertIn("| `service` | 8/10 | 80.0 % | >= 80 % PASS |", table)
        self.assertIn("| `api` | 1/10 | 10.0 % | - |", table)

    def test_gate_fails_below_minimum_or_when_a_layer_is_missing(self):
        _, failures = cs.summarise(cs.package_lines(csv_file([("service", 3, 7), ("domain", 0, 1)])), 80.0)
        self.assertIn("service: 70.0 % < 80 %", failures)
        self.assertIn("persistence: no coverage data", failures)

    def test_script_exit_codes(self):
        ok = subprocess.run([sys.executable, str(SCRIPT), str(csv_file(
            [("service", 0, 1), ("domain", 0, 1), ("persistence", 0, 1)]))], capture_output=True, text=True)
        self.assertEqual(ok.returncode, 0, ok.stdout)
        self.assertIn("PASS: coverage gate met", ok.stdout)
        missing = subprocess.run([sys.executable, str(SCRIPT), "/nonexistent/jacoco.csv"], capture_output=True, text=True)
        self.assertEqual(missing.returncode, 1)


if __name__ == "__main__":
    unittest.main()
