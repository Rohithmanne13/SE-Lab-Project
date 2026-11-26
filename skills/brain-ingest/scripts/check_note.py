#!/usr/bin/env python3
"""brain-ingest evaluation: check notes produced by the skill against SK-1 (SRS TQ-7).

For each given note (default: every note in brain/inbox/) it verifies that:
  E1  the note is in brain/inbox/;
  E2  front matter has the DR-8 fields with type=research, status=inbox and a
      Team 007 author;
  E3  the body has '## Summary', '## Key points', '## Requirements', '## Source';
  E4  it links to at least one other note and to at least one SRS requirement
      heading ([[srs-requirements#ID]]), and every such link resolves;
  E5  the source reference is a URL or an existing repository file;
  E6  brain/index.md lists the note;
  E7  brain/log.md has a brain-ingest entry for the note.
Source fidelity ("never invents content") is checked by a human reviewer in
the pull request and recorded in docs/testing/skill-evaluations.md.

Usage: python3 skills/brain-ingest/scripts/check_note.py [--vault brain] [note.md ...]
Exit status 0 = every check passed, 1 = at least one failure.
"""
from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(REPO / "scripts" / "ci"))
import brainlib as bl  # noqa: E402

SECTIONS = ("Summary", "Key points", "Requirements", "Source")


def evaluate(note: Path, vault: Path, notes: dict, index_text: str, log_text: str) -> list[tuple[str, bool, str]]:
    results = []
    rel = note.resolve().relative_to(vault.resolve())
    results.append(("E1 in inbox/", rel.parts[0] == "inbox", str(rel)))

    text = note.read_text(encoding="utf-8")
    try:
        fields, body = bl.parse_front_matter(text)
        errs = bl.check_note_front_matter(fields)
        if fields.get("type") != "research":
            errs.append("type must be 'research'")
        if fields.get("status") != "inbox":
            errs.append("status must be 'inbox'")
    except bl.FrontMatterError as exc:
        fields, body, errs = {}, text, [str(exc)]
    results.append(("E2 DR-8 front matter", not errs, "; ".join(errs) or "ok"))

    found = bl.headings(body)
    missing = [s for s in SECTIONS if s not in found]
    results.append(("E3 required sections", not missing, f"missing {missing}" if missing else "ok"))

    links = bl.wikilinks(body)
    req_links = [(t, a) for t, a in links if t == "srs-requirements" and a]
    other = [t for t, a in links if t and (t != "srs-requirements" or not a)]
    broken = [t for t, _ in links if t and bl.resolve(t, notes, vault) is None]
    req_heads = bl.headings(notes["srs-requirements"].read_text(encoding="utf-8")) if "srs-requirements" in notes else set()
    bad_req = [a for _, a in req_links if a not in req_heads]
    ok4 = bool(req_links) and bool(other) and not broken and not bad_req
    results.append(("E4 links (note + requirement)", ok4,
                    f"requirement links={len(req_links)}, note links={len(other)}, broken={broken + bad_req}"))

    src_section = body.split("## Source", 1)[-1] if "## Source" in body else ""
    src = re.search(r"^- (\S+)", src_section, re.M)
    src_ok = bool(src) and (src.group(1).startswith(("http://", "https://")) or (REPO / src.group(1)).exists())
    results.append(("E5 source reference", src_ok, src.group(1) if src else "no '- <source>' line"))

    results.append(("E6 listed in index.md", f"[[{note.stem}]]" in index_text, "ok" if f"[[{note.stem}]]" in index_text else "not listed"))
    logged = bool(re.search(rf"\| brain-ingest \| created \[\[{re.escape(note.stem)}\]\]", log_text))
    results.append(("E7 logged in log.md", logged, "ok" if logged else "no brain-ingest entry"))
    return results


def main(argv: list[str]) -> int:
    ap = argparse.ArgumentParser(description="Evaluate brain-ingest output against SK-1.")
    ap.add_argument("--vault", default="brain")
    ap.add_argument("notes", nargs="*")
    a = ap.parse_args(argv[1:])
    vault = Path(a.vault)
    notes = bl.vault_notes(vault)
    targets = [Path(n) for n in a.notes] or sorted((vault / "inbox").glob("*.md"))
    if not targets:
        print("PASS: no notes in brain/inbox/ to evaluate (all notes have been refined into PARA folders)")
        return 0
    index_text = (vault / "index.md").read_text(encoding="utf-8")
    log_text = (vault / "log.md").read_text(encoding="utf-8")
    failed = 0
    for note in targets:
        res = evaluate(note, vault, notes, index_text, log_text)
        ok = all(r[1] for r in res)
        failed += not ok
        print(f"{'PASS' if ok else 'FAIL'}  {note.as_posix()}")
        for name, passed, detail in res:
            if not passed:
                print(f"      {name}: {detail}")
    print(f"{'PASS' if not failed else 'FAIL'}: {len(targets) - failed}/{len(targets)} notes passed checks E1-E7")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
