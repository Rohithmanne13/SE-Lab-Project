#!/usr/bin/env python3
"""Validate every skill folder (SRS PR-5, SK-0, SB-3; Agent Skills format [15]).

For each skills/<skill-name>/SKILL.md:
  * YAML front matter with 'name' and 'description' (Agent Skills spec):
      - name: 1-64 chars, lowercase a-z / 0-9 / hyphens, no leading, trailing
        or consecutive hyphens, identical to the folder name;
      - description: 1-1024 chars;
      - only spec fields are allowed (license, compatibility, metadata, allowed-tools);
  * the body states Inputs, Steps, Outputs, Failure handling and Evaluation (SK-0);
  * the body cites at least one distilled brain note via [[wikilink]] (SB-3).

Usage: python3 scripts/ci/validate_skills.py [skills_dir] [vault_dir]
Exit status 0 = valid, 1 = violations found.
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import brainlib as bl  # noqa: E402

NAME_RE = re.compile(r"^[a-z0-9]+(-[a-z0-9]+)*$")
ALLOWED_FIELDS = {"name", "description", "license", "compatibility", "metadata", "allowed-tools"}
REQUIRED_SECTIONS = ("Inputs", "Steps", "Outputs", "Failure handling", "Evaluation")


def check_skill(skill_dir: Path, notes: dict, vault: Path) -> list[str]:
    errors: list[str] = []
    skill_md = skill_dir / "SKILL.md"
    if not skill_md.is_file():
        return [f"{skill_dir}: missing SKILL.md"]
    text = skill_md.read_text(encoding="utf-8")
    try:
        fields, body = bl.parse_front_matter(text)
    except bl.FrontMatterError as exc:
        return [f"{skill_md}: {exc}"]

    name = fields.get("name")
    if not isinstance(name, str) or not name:
        errors.append(f"{skill_md}: front matter must have a 'name'")
    else:
        if len(name) > 64 or not NAME_RE.match(name):
            errors.append(f"{skill_md}: name '{name}' must be 1-64 chars of a-z, 0-9 and single hyphens, "
                          "not starting or ending with a hyphen")
        if name != skill_dir.name:
            errors.append(f"{skill_md}: name '{name}' must equal the folder name '{skill_dir.name}'")
    desc = fields.get("description")
    if not isinstance(desc, str) or not desc.strip():
        errors.append(f"{skill_md}: front matter must have a non-empty 'description'")
    elif len(desc) > 1024:
        errors.append(f"{skill_md}: description is {len(desc)} chars; maximum is 1024")
    if isinstance(fields.get("compatibility"), str) and len(fields["compatibility"]) > 500:
        errors.append(f"{skill_md}: compatibility is longer than 500 chars")
    unknown = sorted(set(fields) - ALLOWED_FIELDS)
    if unknown:
        errors.append(f"{skill_md}: unknown front-matter field(s) {unknown}; allowed: {sorted(ALLOWED_FIELDS)}")

    found = bl.headings(body)
    for section in REQUIRED_SECTIONS:
        if section not in found:
            errors.append(f"{skill_md}: missing section '## {section}' (SK-0)")

    distilled = []
    for target, _anchor in bl.wikilinks(body):
        dest = bl.resolve(target, notes, vault) if target else None
        if dest is None:
            errors.append(f"{skill_md}: cites [[{target}]], which is not a note in {vault}/")
            continue
        try:
            note_fields, _ = bl.parse_front_matter(dest.read_text(encoding="utf-8"))
        except bl.FrontMatterError:
            continue
        if note_fields.get("status") == "distilled":
            distilled.append(target)
    if not distilled:
        errors.append(f"{skill_md}: must cite at least one distilled brain note with [[...]] (SB-3)")
    return errors


def main(argv: list[str]) -> int:
    skills = Path(argv[1] if len(argv) > 1 else "skills")
    vault = Path(argv[2] if len(argv) > 2 else "brain")
    if not skills.is_dir():
        print(f"ERROR: skills folder '{skills}' not found")
        return 1
    try:
        notes = bl.vault_notes(vault) if vault.is_dir() else {}
    except ValueError as exc:
        print(f"ERROR: {exc}")
        return 1
    skill_dirs = sorted(p for p in skills.iterdir() if p.is_dir() and not p.name.startswith("."))
    errors: list[str] = []
    if not skill_dirs:
        errors.append(f"{skills}/: no skill folders found")
    for d in skill_dirs:
        errors += check_skill(d, notes, vault)
    for e in errors:
        print(f"ERROR: {e}")
    status = "FAIL" if errors else "PASS"
    print(f"{status}: {len(skill_dirs)} skill(s) checked, {len(errors)} error(s)")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
