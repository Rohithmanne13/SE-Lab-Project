#!/usr/bin/env python3
"""brain-refine helper: file inbox notes into PARA, merge duplicates, repair links (SRS SK-2).

Sub-commands (run from the repository root):
  status                         list inbox notes, notes per PARA folder, broken links
  move <note> <target>           move a note to a PARA folder and mark it refined:
                                 target = projects/<name> | areas[/<name>] | resources[/<name>] | archive
                                 status becomes 'distilled' ('archived' for archive);
                                 the note's line moves to the matching section of brain/index.md
  keep <note> --reason "..."     leave a note in inbox/ on purpose, with a reason (shown in index.md)
  merge <keep> <duplicate>       the duplicate goes to archive/ ('Merged into [[keep]]') and every
                                 link to it is rewritten to point at <keep>
  relink <old> <new>             rewrite every [[old...]] link in the vault to [[new...]]
  check                          SK-2 acceptance: inbox empty or every remaining note kept with a
                                 reason, and zero broken links (exit 1 otherwise)

Every change needs --by "<Team 007 member>" and appends one line to brain/log.md. Nothing is written unless all checks pass.
Standard library only; shares the parser in scripts/ci/brainlib.py with the CI validator.
"""
from __future__ import annotations

import argparse
import datetime as dt
import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(REPO / "scripts" / "ci"))
import brainlib as bl  # noqa: E402

TOP_TITLES = {"projects": "## Projects", "areas": "## Areas", "resources": "## Resources", "archive": "## Archive"}
PLACEHOLDER = "_(none yet)_"


class RefineError(Exception):
    pass


def now() -> str:
    return dt.datetime.now().strftime("%Y-%m-%d %H:%M")


RUN_BY = ""  # set from --by in main(); a Team 007 member (SB-6)


def log(vault: Path, what: str, detail: str) -> None:
    with (vault / "log.md").open("a", encoding="utf-8") as fh:
        fh.write(f"- {now()} | brain-refine | {what} | {RUN_BY} | {detail or '-'}\n")


def find_note(vault: Path, name: str) -> Path:
    notes = bl.vault_notes(vault)
    if name not in notes:
        raise RefineError(f"no note named '{name}' in {vault}/")
    path = notes[name]
    if path.relative_to(vault).as_posix() in bl.SPECIAL_FILES or name == "srs-requirements":
        raise RefineError(f"'{name}' is a special note and is never moved")
    return path


def parse_target(target: str) -> tuple[str, str]:
    parts = [p for p in target.strip("/").split("/") if p]
    if not parts or parts[0] not in TOP_TITLES:
        raise RefineError(f"target must start with one of {sorted(TOP_TITLES)} (got '{target}')")
    if parts[0] == "projects" and len(parts) < 2:
        raise RefineError("a projects target needs the project name, e.g. projects/financial-ledger")
    if parts[0] == "archive" and len(parts) > 1:
        raise RefineError("archive takes no subfolder")
    for p in parts[1:]:
        if not re.fullmatch(r"[a-z0-9]+(-[a-z0-9]+)*", p):
            raise RefineError(f"subfolder '{p}' must be lowercase words joined by hyphens")
    if len(parts) > 2:
        raise RefineError("at most one subfolder level, e.g. resources/sqlite")
    return parts[0], (parts[1] if len(parts) > 1 else "")


def set_status(text: str, status: str) -> str:
    new, n = re.subn(r"^status: .*$", f"status: {status}", text, count=1, flags=re.M)
    if n != 1:
        raise RefineError("note has no 'status:' front-matter line")
    return new


def index_line_for(index_lines: list[str], name: str) -> int | None:
    for i, line in enumerate(index_lines):
        if line.startswith(f"- [[{name}]]"):
            return i
    return None


def section_bounds(lines: list[str], heading: str, start: int = 0, end: int | None = None) -> tuple[int, int] | None:
    """Return (heading_index, end_index_exclusive) of the section with exactly this heading line."""
    end = len(lines) if end is None else end
    level = len(heading) - len(heading.lstrip("#"))
    for i in range(start, end):
        if lines[i].strip() == heading:
            j = i + 1
            while j < end:
                m = re.match(r"^(#{1,6}) ", lines[j])
                if m and len(m.group(1)) <= level:
                    break
                j += 1
            return i, j
    return None


def insert_into_section(lines: list[str], top: str, sub: str, entry: str) -> list[str]:
    top_heading = TOP_TITLES[top]
    bounds = section_bounds(lines, top_heading)
    if bounds is None:
        raise RefineError(f"brain/index.md has no '{top_heading}' section")
    t0, t1 = bounds
    if sub:
        sb = section_bounds(lines, f"### {sub}", t0 + 1, t1)
        if sb is None:
            # create the subsection at the end of the top section
            k = t1
            while k > t0 + 1 and lines[k - 1].strip() == "":
                k -= 1
            lines[k:k] = ["", f"### {sub}", ""]
            t1 = section_bounds(lines, top_heading)[1]
            sb = section_bounds(lines, f"### {sub}", t0 + 1, t1)
        s0, s1 = sb
    else:
        s0 = t0
        first_sub = next((k for k in range(t0 + 1, t1) if lines[k].startswith("### ")), t1)
        s1 = first_sub
    # drop the placeholder of the top section (it may sit above a new subsection)
    for k in range(t0 + 1, len(lines)):
        if re.match(r"^#{1,2} ", lines[k]):
            break
        if lines[k].strip() == PLACEHOLDER:
            del lines[k]
            if k < s0:
                s0 -= 1
            s1 -= 1
            break
    k = s1
    while k > s0 + 1 and lines[k - 1].strip() == "":
        k -= 1
    if k == s0 + 1:  # empty section: keep one blank line under the heading
        lines[k:k] = ["", entry]
    else:
        lines[k:k] = [entry]
    return lines


def ensure_placeholder(lines: list[str], heading: str) -> list[str]:
    """Put '_(none yet)_' back into a top section that has no entries left."""
    bounds = section_bounds(lines, heading)
    if not bounds:
        return lines
    t0, t1 = bounds
    body = [line for line in lines[t0 + 1:t1] if line.strip()]
    if not any(line.startswith("- ") or line.startswith("### ") for line in body) and PLACEHOLDER not in body:
        k = t1
        while k > t0 + 1 and lines[k - 1].strip() == "":
            k -= 1
        lines[k:k] = ["", PLACEHOLDER]
    return lines


def tidy(lines: list[str]) -> str:
    """Join lines, collapsing runs of blank lines to one and ending with a single newline."""
    text = "\n".join(lines)
    text = re.sub(r"\n{3,}", "\n\n", text)
    return text.rstrip("\n") + "\n"


def cmd_move(vault: Path, name: str, target: str) -> str:
    path = find_note(vault, name)
    top, sub = parse_target(target)
    dest_dir = vault / top / sub if sub else vault / top
    dest = dest_dir / path.name
    if dest.resolve() == path.resolve():
        raise RefineError(f"'{name}' is already in {dest_dir.relative_to(vault)}/")
    if dest.exists():
        raise RefineError(f"{dest} already exists")
    status = "archived" if top == "archive" else "distilled"
    text = path.read_text(encoding="utf-8")
    fields, _ = bl.parse_front_matter(text)
    new_text = set_status(text, status)

    index = vault / "index.md"
    lines = index.read_text(encoding="utf-8").split("\n")
    i = index_line_for(lines, name)
    if i is not None:
        entry = re.sub(r" - kept in inbox: .*$", "", lines[i])
        del lines[i]
    else:
        entry = f"- [[{name}]] - {fields.get('title', name)}"
    lines = insert_into_section(lines, top, sub, entry)
    lines = ensure_placeholder(lines, "## Inbox")

    dest_dir.mkdir(parents=True, exist_ok=True)
    dest.write_text(new_text, encoding="utf-8")
    path.unlink()
    index.write_text(tidy(lines), encoding="utf-8")
    rel_from = path.relative_to(vault).parent.as_posix()
    rel_to = dest_dir.relative_to(vault).as_posix()
    log(vault, f"moved [[{name}]] {rel_from}/ -> {rel_to}/, status {status}", "")
    return f"moved {name}: {rel_from}/ -> {rel_to}/ (status: {status})"


def cmd_keep(vault: Path, name: str, reason: str) -> str:
    path = find_note(vault, name)
    if path.relative_to(vault).parts[0] != "inbox":
        raise RefineError(f"'{name}' is not in inbox/")
    if not reason.strip():
        raise RefineError("--reason must not be empty")
    index = vault / "index.md"
    lines = index.read_text(encoding="utf-8").split("\n")
    i = index_line_for(lines, name)
    if i is None:
        raise RefineError(f"'{name}' is not listed in index.md")
    lines[i] = re.sub(r" - kept in inbox: .*$", "", lines[i]) + f" - kept in inbox: {reason.strip()}"
    index.write_text(tidy(lines), encoding="utf-8")
    log(vault, f"kept [[{name}]] in inbox/", f"reason: {reason.strip()}")
    return f"kept {name} in inbox ({reason.strip()})"


def rewrite_links(vault: Path, old: str, new: str) -> int:
    pattern = re.compile(r"\[\[" + re.escape(old) + r"(?=[#|\]\\])")
    changed = 0
    for path in bl.vault_notes(vault).values():
        if path.relative_to(vault).as_posix() in bl.SPECIAL_FILES:
            continue  # index.md is maintained by the skill itself; log.md is history and never rewritten
        text = path.read_text(encoding="utf-8")
        new_text, n = pattern.subn("[[" + new, text)
        if n:
            path.write_text(new_text, encoding="utf-8")
            changed += n
    return changed


def cmd_relink(vault: Path, old: str, new: str) -> str:
    notes = bl.vault_notes(vault)
    if new not in notes:
        raise RefineError(f"target note '{new}' does not exist")
    if old in notes:
        raise RefineError(f"note '{old}' still exists - use merge to combine two existing notes")
    n = rewrite_links(vault, old, new)
    log(vault, f"relinked [[{old}]] -> [[{new}]]", f"{n} link(s) rewritten")
    return f"rewrote {n} link(s) from [[{old}]] to [[{new}]]"


def cmd_merge(vault: Path, keep: str, dup: str) -> str:
    if keep == dup:
        raise RefineError("cannot merge a note into itself")
    keep_path = find_note(vault, keep)
    dup_path = find_note(vault, dup)
    text = dup_path.read_text(encoding="utf-8")
    body_start = text.index("\n---", 3) + 4
    text = text[:body_start] + f"\n> Merged into [[{keep}]] on {dt.date.today().isoformat()}; read that note instead.\n" + text[body_start:]
    dup_path.write_text(text, encoding="utf-8")
    msg = cmd_move(vault, dup, "archive")
    n = rewrite_links(vault, dup, keep)
    # the archived duplicate keeps no self-link problems: its own text now links to <keep>
    log(vault, f"merged [[{dup}]] into [[{keep}]]", f"{n} link(s) rewritten")
    return f"{msg}; rewrote {n} link(s) to [[{keep}]]"


def broken_links(vault: Path) -> list[str]:
    notes = bl.vault_notes(vault)
    problems = []
    for path in notes.values():
        for target, anchor in bl.wikilinks(path.read_text(encoding="utf-8")):
            if not target:
                continue
            dest = bl.resolve(target, notes, vault)
            if dest is None:
                problems.append(f"{path.relative_to(vault)}: [[{target}]]")
            elif anchor and not anchor.startswith("^") and anchor.split("#")[-1] not in bl.headings(dest.read_text(encoding="utf-8")):
                problems.append(f"{path.relative_to(vault)}: [[{target}#{anchor}]]")
    return problems


def cmd_status(vault: Path) -> str:
    notes = bl.vault_notes(vault)
    by_folder: dict[str, list[str]] = {}
    for name, path in notes.items():
        rel = path.relative_to(vault)
        if rel.as_posix() in bl.SPECIAL_FILES:
            continue
        by_folder.setdefault(rel.parent.as_posix(), []).append(name)
    out = []
    for folder in sorted(by_folder):
        out.append(f"{folder}/ ({len(by_folder[folder])}): {', '.join(sorted(by_folder[folder]))}")
    out.append(f"broken links: {len(broken_links(vault))}")
    return "\n".join(out)


def cmd_check(vault: Path) -> tuple[bool, str]:
    index_lines = (vault / "index.md").read_text(encoding="utf-8").split("\n")
    inbox = sorted((vault / "inbox").glob("*.md"))
    unexplained = []
    for p in inbox:
        i = index_line_for(index_lines, p.stem)
        if i is None or " - kept in inbox: " not in index_lines[i]:
            unexplained.append(p.stem)
    broken = broken_links(vault)
    ok = not unexplained and not broken
    lines = [f"inbox notes: {len(inbox)} ({len(inbox) - len(unexplained)} kept with a reason)",
             f"inbox notes without a reason: {unexplained or 'none'}",
             f"broken links: {len(broken)}"] + [f"  {b}" for b in broken]
    lines.append(("PASS" if ok else "FAIL") + ": SK-2 acceptance (inbox empty or every note kept with a reason; zero broken links)")
    return ok, "\n".join(lines)


def main(argv: list[str]) -> int:
    ap = argparse.ArgumentParser(description="Refine the brain/ vault (SK-2).")
    ap.add_argument("--vault", default="brain")
    ap.add_argument("--by", default="", help="Team 007 member running the skill (required for changes)")
    sub = ap.add_subparsers(dest="cmd", required=True)
    sub.add_parser("status")
    sub.add_parser("check")
    p = sub.add_parser("move"); p.add_argument("note"); p.add_argument("target")
    p = sub.add_parser("keep"); p.add_argument("note"); p.add_argument("--reason", required=True)
    p = sub.add_parser("merge"); p.add_argument("keep"); p.add_argument("duplicate")
    p = sub.add_parser("relink"); p.add_argument("old"); p.add_argument("new")
    a = ap.parse_args(argv[1:])
    vault = Path(a.vault)
    global RUN_BY
    RUN_BY = a.by.strip()
    if a.cmd in ("move", "keep", "merge", "relink") and RUN_BY not in bl.TEAM_MEMBERS:
        print(f"brain-refine: REJECTED - --by must be one of {sorted(bl.TEAM_MEMBERS)} (nothing was written)", file=sys.stderr)
        return 1
    if not (vault / "index.md").is_file() or not (vault / "log.md").is_file():
        print(f"brain-refine: REJECTED - {vault}/ is not a vault (index.md / log.md missing)", file=sys.stderr)
        return 1
    try:
        if a.cmd == "status":
            print(cmd_status(vault)); return 0
        if a.cmd == "check":
            ok, report = cmd_check(vault); print(report); return 0 if ok else 1
        if a.cmd == "move":
            print("brain-refine: " + cmd_move(vault, a.note, a.target)); return 0
        if a.cmd == "keep":
            print("brain-refine: " + cmd_keep(vault, a.note, a.reason)); return 0
        if a.cmd == "merge":
            print("brain-refine: " + cmd_merge(vault, a.keep, a.duplicate)); return 0
        if a.cmd == "relink":
            print("brain-refine: " + cmd_relink(vault, a.old, a.new)); return 0
    except (RefineError, bl.FrontMatterError, ValueError) as exc:
        print(f"brain-refine: REJECTED - {exc} (nothing was written)", file=sys.stderr)
        return 1
    return 1


if __name__ == "__main__":
    sys.exit(main(sys.argv))
