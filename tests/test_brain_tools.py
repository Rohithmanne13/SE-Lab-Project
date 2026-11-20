"""Regression tests for the brain tooling (SRS TQ-8: each significant defect gets a regression test).

Run: python3 -m unittest discover -s tests -v
"""
import shutil
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(REPO / "scripts" / "ci"))
import brainlib as bl  # noqa: E402

INGEST = REPO / "skills" / "brain-ingest" / "scripts" / "ingest.py"


class TempVault(unittest.TestCase):
    """Each test works on a throwaway copy of brain/ so the real vault is never touched."""

    def setUp(self):
        self.tmp = Path(tempfile.mkdtemp())
        self.vault = self.tmp / "brain"
        shutil.copytree(REPO / "brain", self.vault, ignore=shutil.ignore_patterns(".obsidian"))

    def tearDown(self):
        shutil.rmtree(self.tmp)

    def ingest(self, body: str, slug: str):
        body_file = self.tmp / "body.md"
        body_file.write_text(body, encoding="utf-8")
        return subprocess.run(
            [sys.executable, str(INGEST), "--vault", str(self.vault), "--title", "Regression note",
             "--slug", slug, "--source", "https://example.org/doc", "--author", "Manne Rohith Sai",
             "--tags", "test", "--related", "SK-1", "--body", str(body_file)],
            capture_output=True, text=True)


class IngestRegressions(TempVault):
    def test_links_inside_inline_code_are_not_treated_as_links(self):
        # Defect 1 (2026-11-20): `[[Example]]` inside inline code made ingest reject a valid note.
        body = ("## Summary\n\nWikilinks look like `[[Three laws of motion]]`.\n\n"
                "## Key points\n\n- Example `[[Note#Heading|text]]`.\n\n## Links\n\n- [[para-method]]\n")
        result = self.ingest(body, "regression-inline-code")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertTrue((self.vault / "inbox" / "regression-inline-code.md").exists())

    def test_existing_slug_is_rejected_and_nothing_written(self):
        before = {p: p.read_bytes() for p in (self.vault / "index.md", self.vault / "log.md")}
        body = "## Summary\n\ns\n\n## Key points\n\n- k\n\n## Links\n\n- [[para-method]]\n"
        result = self.ingest(body, "para-method")
        self.assertEqual(result.returncode, 1)
        self.assertIn("already exists", result.stderr)
        for path, content in before.items():
            self.assertEqual(path.read_bytes(), content, f"{path.name} changed after a rejected ingest")

    def test_created_note_passes_vault_validation(self):
        body = "## Summary\n\ns\n\n## Key points\n\n- k\n\n## Links\n\n- [[para-method]]\n"
        self.assertEqual(self.ingest(body, "regression-valid").returncode, 0)
        result = subprocess.run([sys.executable, str(REPO / "scripts" / "ci" / "validate_brain.py"), str(self.vault)],
                                capture_output=True, text=True)
        self.assertEqual(result.returncode, 0, result.stdout)


class LinkParserRegressions(unittest.TestCase):
    def test_escaped_pipe_in_table_link(self):
        # Defect 2 (2026-11-20): '\|' inside a table link was read as part of the heading name.
        self.assertEqual(bl.wikilinks("| [[srs-requirements#SK-1\\|SK-1]] |"), [("srs-requirements", "SK-1")])

    def test_links_in_code_blocks_are_ignored(self):
        text = "```\n[[not-a-link]]\n```\nand `[[also-not]]` but [[real-link]]"
        self.assertEqual(bl.wikilinks(text), [("real-link", "")])


class FrontMatterParser(unittest.TestCase):
    def test_inline_and_block_lists_and_nested_map(self):
        fields, body = bl.parse_front_matter(
            "---\ntitle: \"A: B\"\ntags: [a, b]\nrelated:\n  - FR-1\n  - SB-1\nmetadata:\n  author: x\n---\nbody")
        self.assertEqual(fields["title"], "A: B")
        self.assertEqual(fields["tags"], ["a", "b"])
        self.assertEqual(fields["related"], ["FR-1", "SB-1"])
        self.assertEqual(fields["metadata"], {"author": "x"})
        self.assertEqual(body, "body")

    def test_missing_front_matter_is_an_error(self):
        with self.assertRaises(bl.FrontMatterError):
            bl.parse_front_matter("# no front matter")


if __name__ == "__main__":
    unittest.main()
