"""Tests for the brain-refine skill script (SRS SK-2, TQ-8).

Run: python3 -m unittest tests.test_brain_refine -v
Every test works on a throwaway copy of brain/, so the real vault is never touched.
"""
import shutil
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]
REFINE = REPO / "skills" / "brain-refine" / "scripts" / "refine.py"
VALIDATE = REPO / "scripts" / "ci" / "validate_brain.py"
BY = "Rudraraju Dhanush Varma"


class RefineTest(unittest.TestCase):
    def setUp(self):
        self.tmp = Path(tempfile.mkdtemp())
        self.vault = self.tmp / "brain"
        shutil.copytree(REPO / "brain", self.vault, ignore=shutil.ignore_patterns(".obsidian"))
        # a fresh inbox note to work with, independent of the real vault's current contents
        (self.vault / "inbox").mkdir(exist_ok=True)
        (self.vault / "inbox" / "test-note-a.md").write_text(
            "---\ntitle: \"Test note A\"\ntype: research\nstatus: inbox\ntags: [test]\ncreated: 2026-11-23\n"
            f"author: {BY}\nrelated: [SK-2]\n---\n\n# Test note A\n\nSee [[second-brain-workflow]].\n", encoding="utf-8")
        (self.vault / "inbox" / "test-note-b.md").write_text(
            "---\ntitle: \"Test note B\"\ntype: research\nstatus: inbox\ntags: [test]\ncreated: 2026-11-23\n"
            f"author: {BY}\nrelated: [SK-2]\n---\n\n# Test note B\n\nDuplicate of A.\n", encoding="utf-8")
        (self.vault / "resources" / "test-linker.md").write_text(
            "---\ntitle: \"Linker\"\ntype: concept\nstatus: distilled\ntags: [test]\ncreated: 2026-11-23\n"
            f"author: {BY}\nrelated: [SK-2]\n---\n\n# Linker\n\nLinks to [[test-note-b]] and [[test-note-b|B]].\n", encoding="utf-8")
        index = self.vault / "index.md"
        text = index.read_text(encoding="utf-8")
        head, sep, tail = text.partition("## Inbox")
        tail = tail.replace("- _(none yet)_\n", "").replace("_(none yet)_\n", "")
        index.write_text(head + sep + tail.replace(
            "PARA folders.\n", "PARA folders.\n\n- [[test-note-a]] - Test note A\n- [[test-note-b]] - Test note B\n", 1),
            encoding="utf-8")

    def tearDown(self):
        shutil.rmtree(self.tmp)

    def run_refine(self, *args, by=BY):
        cmd = [sys.executable, str(REFINE), "--vault", str(self.vault)]
        if by:
            cmd += ["--by", by]
        return subprocess.run(cmd + list(args), capture_output=True, text=True)

    def assert_valid(self):
        r = subprocess.run([sys.executable, str(VALIDATE), str(self.vault)], capture_output=True, text=True)
        self.assertEqual(r.returncode, 0, r.stdout)

    def test_move_sets_status_updates_index_and_log(self):
        r = self.run_refine("move", "test-note-a", "resources/testing")
        self.assertEqual(r.returncode, 0, r.stderr)
        moved = self.vault / "resources" / "testing" / "test-note-a.md"
        self.assertTrue(moved.exists())
        self.assertFalse((self.vault / "inbox" / "test-note-a.md").exists())
        self.assertIn("status: distilled", moved.read_text(encoding="utf-8"))
        index = (self.vault / "index.md").read_text(encoding="utf-8")
        res = index.split("## Resources", 1)[1].split("## Inbox", 1)[0]
        self.assertIn("### testing", res)
        self.assertIn("- [[test-note-a]]", res)
        self.assertNotIn("[[test-note-a]]", index.split("## Inbox", 1)[1])
        self.assertIn(f"| brain-refine | moved [[test-note-a]] inbox/ -> resources/testing/, status distilled | {BY} |",
                      (self.vault / "log.md").read_text(encoding="utf-8"))
        self.assert_valid()

    def test_move_to_archive_sets_archived(self):
        self.assertEqual(self.run_refine("move", "test-note-a", "archive").returncode, 0)
        self.assertIn("status: archived", (self.vault / "archive" / "test-note-a.md").read_text(encoding="utf-8"))
        self.assert_valid()

    def test_keep_with_reason_counts_for_check(self):
        self.run_refine("move", "test-note-b", "resources/testing")
        for p in (self.vault / "inbox").glob("*.md"):
            if p.stem != "test-note-a":
                self.run_refine("move", p.stem, "resources/testing")
        self.assertEqual(self.run_refine("check", by="").returncode, 1)  # test-note-a has no reason yet
        self.assertEqual(self.run_refine("keep", "test-note-a", "--reason", "waiting for review").returncode, 0)
        r = self.run_refine("check", by="")
        self.assertEqual(r.returncode, 0, r.stdout)
        self.assertIn("kept in inbox: waiting for review", (self.vault / "index.md").read_text(encoding="utf-8"))

    def test_merge_archives_duplicate_and_rewrites_links(self):
        r = self.run_refine("merge", "test-note-a", "test-note-b")
        self.assertEqual(r.returncode, 0, r.stderr)
        archived = (self.vault / "archive" / "test-note-b.md").read_text(encoding="utf-8")
        self.assertIn("status: archived", archived)
        self.assertIn("Merged into [[test-note-a]]", archived)
        linker = (self.vault / "resources" / "test-linker.md").read_text(encoding="utf-8")
        self.assertIn("[[test-note-a]]", linker)
        self.assertIn("[[test-note-a|B]]", linker)
        self.assertNotIn("[[test-note-b", linker)
        self.assert_valid()

    def test_relink_repairs_links_to_a_missing_note(self):
        # simulate a renamed note: links still point at the old name, which no longer exists
        (self.vault / "inbox" / "test-note-b.md").unlink()
        r = self.run_refine("relink", "test-note-b", "test-note-a")
        self.assertEqual(r.returncode, 0, r.stderr)
        self.assertIn("rewrote 2 link(s)", r.stdout)  # the two links in test-linker; index.md and log.md untouched
        self.assertIn("[[test-note-b]]", (self.vault / "index.md").read_text(encoding="utf-8"))

    def test_merge_keeps_index_entry_of_the_archived_duplicate(self):
        self.run_refine("merge", "test-note-a", "test-note-b")
        archive = (self.vault / "index.md").read_text(encoding="utf-8").split("## Archive", 1)[1]
        self.assertIn("- [[test-note-b]] - Test note B", archive)

    def test_refusals_write_nothing(self):
        before = {p: p.read_bytes() for p in self.vault.rglob("*.md")}
        cases = [
            (("move", "no-such-note", "resources"), BY),
            (("move", "test-note-a", "projects"), BY),          # project name missing
            (("move", "test-note-a", "elsewhere/x"), BY),       # not a PARA folder
            (("move", "test-note-a", "resources/Bad_Name"), BY),
            (("move", "srs-requirements", "archive"), BY),       # special note
            (("move", "index", "archive"), BY),
            (("move", "test-note-a", "resources"), "Someone Else"),
            (("move", "test-note-a", "resources"), ""),          # --by missing
            (("keep", "test-note-a", "--reason", " "), BY),
            (("relink", "test-note-b", "missing-target"), BY),
            (("relink", "test-note-b", "test-note-a"), BY),      # old note still exists: use merge
            (("merge", "test-note-a", "test-note-a"), BY),
        ]
        for args, by in cases:
            with self.subTest(args=args, by=by):
                r = self.run_refine(*args, by=by)
                self.assertEqual(r.returncode, 1, r.stdout + r.stderr)
                self.assertIn("REJECTED", r.stderr)
        after = {p: p.read_bytes() for p in self.vault.rglob("*.md")}
        self.assertEqual(before, after)


if __name__ == "__main__":
    unittest.main()
