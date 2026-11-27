#!/usr/bin/env python3
"""Validate Architecture Decision Records in docs/adr/ (SRS SE-3, SB-3).

For every docs/adr/NNNN-<title>.md:
  * the first line is '# ADR-NNNN: <title>' with NNNN matching the file name;
  * sections Status, Context, Decision, Alternatives considered, Consequences, Sources exist;
  * the Status section starts with Proposed, Accepted or Superseded;
  * it cites at least one *distilled* brain note as a [[wikilink]] (SB-3), and every
    [[wikilink]] in it resolves to a note in brain/;
  * docs/adr/README.md lists it.

Usage: python3 scripts/ci/validate_adrs.py [adr_dir] [vault_dir]
Exit status 0 = valid, 1 = violations found.
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import brainlib as bl  # noqa: E402

SECTIONS = ("Status", "Context", "Decision", "Alternatives considered", "Consequences", "Sources")
NAME_RE = re.compile(r"^(\d{4})-[a-z0-9]+(-[a-z0-9]+)*\.md$")


def check_adr(path: Path, notes: dict, vault: Path, readme: str) -> list[str]:
    errors = []
    m = NAME_RE.match(path.name)
    if not m:
        return [f"{path}: file name must be NNNN-lowercase-words.md"]
    number = m.group(1)
    text = path.read_text(encoding="utf-8")
    first = text.splitlines()[0] if text.strip() else ""
    if not re.match(rf"^# ADR-{number}: \S", first):
        errors.append(f"{path}: first line must be '# ADR-{number}: <title>'")
    found = bl.headings(text)
    for s in SECTIONS:
        if s not in found:
            errors.append(f"{path}: missing section '## {s}'")
    status = re.search(r"^## Status\s*\n+(.+)$", text, re.M)
    if status and not re.match(r"(Proposed|Accepted|Superseded)\b", status.group(1).strip()):
        errors.append(f"{path}: Status must start with Proposed, Accepted or Superseded")
    distilled = 0
    for target, anchor in bl.wikilinks(text):
        dest = bl.resolve(target, notes, vault) if target else None
        if dest is None:
            errors.append(f"{path}: [[{target}]] is not a note in {vault}/")
            continue
        if anchor and anchor.split("#")[-1] not in bl.headings(dest.read_text(encoding="utf-8")):
            errors.append(f"{path}: [[{target}#{anchor}]] - heading not found")
        try:
            fields, _ = bl.parse_front_matter(dest.read_text(encoding="utf-8"))
        except bl.FrontMatterError:
            continue
        distilled += fields.get("status") == "distilled"
    if not distilled:
        errors.append(f"{path}: must cite at least one distilled brain note with [[...]] (SB-3)")
    if f"({path.name})" not in readme:
        errors.append(f"{path}: not listed in docs/adr/README.md")
    return errors


def main(argv: list[str]) -> int:
    adr_dir = Path(argv[1] if len(argv) > 1 else "docs/adr")
    vault = Path(argv[2] if len(argv) > 2 else "brain")
    if not adr_dir.is_dir():
        print(f"PASS: no ADR folder '{adr_dir}' yet - nothing to check")
        return 0
    try:
        notes = bl.vault_notes(vault)
    except ValueError as exc:
        print(f"ERROR: {exc}")
        return 1
    readme_path = adr_dir / "README.md"
    readme = readme_path.read_text(encoding="utf-8") if readme_path.is_file() else ""
    adrs = sorted(p for p in adr_dir.glob("*.md") if p.name != "README.md")
    errors: list[str] = []
    if adrs and not readme:
        errors.append(f"{readme_path}: missing ADR index")
    numbers = [p.name[:4] for p in adrs]
    if len(numbers) != len(set(numbers)):
        errors.append(f"{adr_dir}: two ADRs share a number")
    for p in adrs:
        errors += check_adr(p, notes, vault, readme)
    for e in errors:
        print(f"ERROR: {e}")
    print(f"{'FAIL' if errors else 'PASS'}: {len(adrs)} ADR(s) checked, {len(errors)} error(s)")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
