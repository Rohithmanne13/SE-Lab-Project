"""Tests for the ADR validator (SRS SE-3, SB-3, TQ-8).

Run: python3 -m unittest tests.test_validate_adrs -v
"""
import shutil
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]
VALIDATE = REPO / "scripts" / "ci" / "validate_adrs.py"


class ValidateAdrsTest(unittest.TestCase):
    def setUp(self):
        self.tmp = Path(tempfile.mkdtemp())
        self.adr = self.tmp / "adr"
        shutil.copytree(REPO / "docs" / "adr", self.adr)
        self.first = sorted(p for p in self.adr.glob("0*.md"))[0]

    def tearDown(self):
        shutil.rmtree(self.tmp)

    def run_validator(self):
        return subprocess.run([sys.executable, str(VALIDATE), str(self.adr), str(REPO / "brain")],
                              capture_output=True, text=True)

    def test_repository_adrs_are_valid(self):
        r = self.run_validator()
        self.assertEqual(r.returncode, 0, r.stdout)

    def test_missing_section_is_reported(self):
        self.first.write_text(self.first.read_text(encoding="utf-8").replace("## Consequences", "## Results"), encoding="utf-8")
        r = self.run_validator()
        self.assertEqual(r.returncode, 1)
        self.assertIn("missing section '## Consequences'", r.stdout)

    def test_adr_without_distilled_citation_is_reported(self):
        text = self.first.read_text(encoding="utf-8")
        body = "\n".join(line for line in text.splitlines() if "[[" not in line)
        self.first.write_text(body + "\n", encoding="utf-8")
        r = self.run_validator()
        self.assertEqual(r.returncode, 1)
        self.assertIn("must cite at least one distilled brain note", r.stdout)

    def test_broken_note_link_is_reported(self):
        self.first.write_text(self.first.read_text(encoding="utf-8") + "\n- [[no-such-note]]\n", encoding="utf-8")
        r = self.run_validator()
        self.assertEqual(r.returncode, 1)
        self.assertIn("[[no-such-note]] is not a note", r.stdout)

    def test_unlisted_adr_is_reported(self):
        new = self.adr / "0099-unlisted-decision.md"
        new.write_text(self.first.read_text(encoding="utf-8").replace(
            self.first.read_text(encoding="utf-8").splitlines()[0], "# ADR-0099: Unlisted decision"), encoding="utf-8")
        r = self.run_validator()
        self.assertEqual(r.returncode, 1)
        self.assertIn("not listed in docs/adr/README.md", r.stdout)


if __name__ == "__main__":
    unittest.main()
