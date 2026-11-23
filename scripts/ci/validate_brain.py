#!/usr/bin/env python3
"""Validate the brain/ Obsidian vault (SRS PR-5, SB-0, SB-1, DR-8).

Checks:
  * PARA folders and the special files index.md / log.md exist (SB-0);
  * every note starts with DR-8 front matter (title, type, status, tags,
    created, author, related) with allowed values;
  * notes in inbox/ have status 'inbox'; distilled/archived notes are outside inbox/;
  * every [[wikilink]] resolves to an existing note, and every #heading
    anchor exists in the target note (no broken links, SB-1);
  * note names are unique, so links are never ambiguous.

Usage: python3 scripts/ci/validate_brain.py [vault_dir]   (default: brain)
Exit status 0 = valid, 1 = violations found.
"""
from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import brainlib as bl  # noqa: E402


def main(argv: list[str]) -> int:
    vault = Path(argv[1] if len(argv) > 1 else "brain")
    errors: list[str] = []

    if not vault.is_dir():
        print(f"ERROR: vault folder '{vault}' not found")
        return 1
    for folder in bl.PARA_FOLDERS:
        if not (vault / folder).is_dir():
            errors.append(f"{vault}/{folder}/: PARA folder missing (SB-0)")
    for special in bl.SPECIAL_FILES:
        if not (vault / special).is_file():
            errors.append(f"{vault}/{special}: required file missing (SB-0)")

    try:
        notes = bl.vault_notes(vault)
    except ValueError as exc:
        print(f"ERROR: {exc}")
        return 1

    heading_cache: dict[Path, set[str]] = {}
    link_count = 0
    for name, path in notes.items():
        rel = path.relative_to(vault)
        text = path.read_text(encoding="utf-8")
        is_special = rel.as_posix() in bl.SPECIAL_FILES
        if not is_special:
            try:
                fields, _ = bl.parse_front_matter(text)
            except bl.FrontMatterError as exc:
                errors.append(f"{path}: {exc} (DR-8)")
                fields = None
            if fields is not None:
                errors += [f"{path}: {e} (DR-8)" for e in bl.check_note_front_matter(fields)]
                top = rel.parts[0] if len(rel.parts) > 1 else ""
                if top == "inbox" and fields.get("status") != "inbox":
                    errors.append(f"{path}: notes in inbox/ must have status 'inbox'")
                if top != "inbox" and fields.get("status") == "inbox":
                    errors.append(f"{path}: status 'inbox' is only allowed for notes in inbox/")
                if top not in bl.PARA_FOLDERS:
                    errors.append(f"{path}: note must live inside a PARA folder {bl.PARA_FOLDERS}")
        for target, anchor in bl.wikilinks(text):
            link_count += 1
            if not target:  # [[#heading]] inside the same note
                dest = path
            else:
                dest = bl.resolve(target, notes, vault)
                if dest is None:
                    errors.append(f"{path}: broken link [[{target}]] - no such note")
                    continue
            if anchor:
                if dest not in heading_cache:
                    heading_cache[dest] = bl.headings(dest.read_text(encoding="utf-8"))
                if anchor.startswith("^"):
                    continue  # block reference; not used by our conventions
                if anchor.split("#")[-1] not in heading_cache[dest]:
                    errors.append(f"{path}: broken link [[{target}#{anchor}]] - heading not found in {dest.name}")

    for e in errors:
        print(f"ERROR: {e}")
    status = "FAIL" if errors else "PASS"
    print(f"{status}: brain vault '{vault}' - {len(notes)} notes, {link_count} links checked, {len(errors)} error(s)")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
