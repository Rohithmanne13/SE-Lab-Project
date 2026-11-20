#!/usr/bin/env python3
"""brain-ingest helper: file one research note into brain/inbox/ (SRS SK-1).

The agent writes the note body (Summary, Key points, Links) from the source;
this script does the mechanical parts so every run is identical:
  1. builds DR-8 front matter (title, type=research, status=inbox, tags,
     created, author, related);
  2. appends '## Requirements' (wikilinks to the SRS requirement headings) and
     '## Source' sections;
  3. writes brain/inbox/<slug>.md, refusing to overwrite an existing note;
  4. adds the note under '## Inbox' in brain/index.md;
  5. appends a dated entry to brain/log.md.
Nothing is written unless every check passes.

Usage:
  python3 skills/brain-ingest/scripts/ingest.py \
      --title "SQLite transactions" --source https://www.sqlite.org/lang_transaction.html \
      --author "Manne Rohith Sai" --tags sqlite,transactions --related FR-3,FR-3.1,CON-2 \
      --body /path/to/body.md [--slug sqlite-transactions] [--vault brain] [--date 2026-11-20]

Standard library only. Exit status 0 = note created, 1 = rejected (nothing written).
"""
from __future__ import annotations

import argparse
import datetime as dt
import re
import sys
from pathlib import Path

TEAM_MEMBERS = ("Manne Rohith Sai", "Sunnapu Suhith Reddy", "Rudraraju Dhanush Varma", "Abhiram P B S")
REQ_INDEX_NOTE = "srs-requirements"          # brain/projects/financial-ledger/srs-requirements.md
REF_RE = re.compile(r"^(#\d+|PR #\d+)$")       # issue / pull-request references (SB-1)
REQUIRED_BODY_SECTIONS = ("## Summary", "## Key points")
WIKILINK_RE = re.compile(r"\[\[([^\]\n]+?)\]\]")


def strip_code(text: str) -> str:
    """Drop fenced code blocks and `inline code`, so link examples are not treated as links."""
    out, fence = [], False
    for line in text.splitlines():
        if line.strip().startswith(("```", "~~~")):
            fence = not fence
            continue
        out.append("" if fence else re.sub(r"`[^`\n]*`", "", line))
    return "\n".join(out)


def fail(msg: str) -> int:
    print(f"brain-ingest: REJECTED - {msg} (nothing was written)", file=sys.stderr)
    return 1


def slugify(title: str) -> str:
    slug = re.sub(r"[^a-z0-9]+", "-", title.lower()).strip("-")
    return re.sub(r"-{2,}", "-", slug)


def requirement_headings(vault: Path) -> set[str]:
    hits = list(vault.rglob(f"{REQ_INDEX_NOTE}.md"))
    if len(hits) != 1:
        return set()
    return {m.group(1).strip() for m in re.finditer(r"^#{1,6}\s+(\S+)\s*$", hits[0].read_text(encoding="utf-8"), re.M)}


def note_names(vault: Path) -> set[str]:
    return {p.stem for p in vault.rglob("*.md") if not any(s.startswith(".") for s in p.relative_to(vault).parts)}


def yaml_str(s: str) -> str:
    return '"' + s.replace("\\", "\\\\").replace('"', '\\"') + '"'


def main(argv: list[str]) -> int:
    ap = argparse.ArgumentParser(description="Create one research note in brain/inbox/ (SK-1).")
    ap.add_argument("--title", required=True)
    ap.add_argument("--source", required=True, help="URL or repository path of the source")
    ap.add_argument("--author", required=True, help="Team 007 member running the skill")
    ap.add_argument("--tags", required=True, help="comma-separated, e.g. sqlite,transactions")
    ap.add_argument("--related", required=True, help="comma-separated requirement IDs and #issue / PR #n refs")
    ap.add_argument("--body", required=True, help="Markdown file with ## Summary, ## Key points, ## Links")
    ap.add_argument("--slug")
    ap.add_argument("--vault", default="brain")
    ap.add_argument("--date", default=dt.date.today().isoformat())
    a = ap.parse_args(argv[1:])

    vault = Path(a.vault)
    inbox, index, log = vault / "inbox", vault / "index.md", vault / "log.md"
    for p in (inbox, index, log):
        if not p.exists():
            return fail(f"vault is not set up: {p} is missing")
    if a.author not in TEAM_MEMBERS:
        return fail(f"author must be one of {TEAM_MEMBERS}")
    try:
        dt.date.fromisoformat(a.date)
    except ValueError:
        return fail(f"--date must be YYYY-MM-DD, got {a.date!r}")

    title = a.title.strip()
    slug = a.slug or slugify(title)
    if not re.fullmatch(r"[a-z0-9]+(-[a-z0-9]+)*", slug):
        return fail(f"slug {slug!r} must be lowercase words joined by single hyphens")
    target = inbox / f"{slug}.md"
    existing = note_names(vault)
    if target.exists() or slug in existing:
        return fail(f"a note named '{slug}' already exists - choose another --slug (no overwrite)")

    tags = [t.strip() for t in a.tags.split(",") if t.strip()]
    if not tags or any(not re.fullmatch(r"[a-z0-9][a-z0-9-]*", t) for t in tags):
        return fail("tags must be lowercase words/hyphens, e.g. sqlite,write-ahead-log")

    related = [r.strip() for r in a.related.split(",") if r.strip()]
    req_ids = [r for r in related if not REF_RE.match(r)]
    known = requirement_headings(vault)
    if not known:
        return fail(f"cannot find the requirement index note '{REQ_INDEX_NOTE}.md' in the vault")
    unknown = [r for r in req_ids if r not in known]
    if unknown:
        return fail(f"unknown requirement ID(s) {unknown}; use IDs that exist in the submitted SRS")
    if not req_ids:
        return fail("--related must contain at least one SRS requirement ID (SB-1)")

    body_path = Path(a.body)
    if not body_path.is_file():
        return fail(f"body file {body_path} not found")
    body = body_path.read_text(encoding="utf-8").strip()
    for section in REQUIRED_BODY_SECTIONS:
        if section not in body:
            return fail(f"body must contain a '{section}' section")
    note_links = [l.split("|")[0].split("#")[0].strip() for l in WIKILINK_RE.findall(strip_code(body))]
    missing = sorted({l for l in note_links if l and l not in existing})
    if missing:
        return fail(f"body links to notes that do not exist: {missing}")

    req_lines = "\n".join(f"- [[{REQ_INDEX_NOTE}#{r}|{r}]]" for r in req_ids)
    refs = [r for r in related if REF_RE.match(r)]
    ref_line = f"\nIssues / PRs: {', '.join(refs)}\n" if refs else ""
    note = (
        "---\n"
        f"title: {yaml_str(title)}\n"
        "type: research\n"
        "status: inbox\n"
        f"tags: [{', '.join(tags)}]\n"
        f"created: {a.date}\n"
        f"author: {a.author}\n"
        f"related: [{', '.join(related)}]\n"
        "---\n\n"
        f"# {title}\n\n"
        f"{body}\n\n"
        "## Requirements\n\n"
        f"{req_lines}\n{ref_line}\n"
        "## Source\n\n"
        f"- {a.source}\n"
        f"- Ingested {a.date} by {a.author} with the brain-ingest skill.\n"
    )

    index_text = index.read_text(encoding="utf-8")
    marker = "## Inbox"
    if marker not in index_text:
        return fail(f"{index} has no '{marker}' section")
    head, _, tail = index_text.partition(marker)
    section, sep, rest = tail.partition("\n## ")
    entry = f"- [[{slug}]] - {title} ({a.date}, {a.author})"
    section = section.replace("\n- _(empty)_", "").rstrip("\n")
    last = section.splitlines()[-1] if section.strip() else ""
    section += ("\n" if last.startswith("- [[") else "\n\n") + entry + "\n"
    new_index = head + marker + section + ("\n" + sep.lstrip("\n") + rest if sep else "")

    stamp = dt.datetime.now().strftime("%H:%M")
    log_entry = f"- {a.date} {stamp} | brain-ingest | created [[{slug}]] in inbox/ | {a.author} | source: {a.source}\n"

    target.write_text(note, encoding="utf-8")
    index.write_text(new_index, encoding="utf-8")
    with log.open("a", encoding="utf-8") as fh:
        fh.write(log_entry)
    print(f"brain-ingest: created {target}; updated {index} and {log}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
