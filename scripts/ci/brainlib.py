"""Shared helpers for the CI validators (SRS PR-5, DR-8, SB-1, SK-0, SB-3).

Standard library only, so CI needs no extra dependencies (SEC-9).
"""
from __future__ import annotations

import datetime as _dt
import re
from pathlib import Path

# DR-8: allowed values for the brain-note front matter.
NOTE_TYPES = {"research", "concept", "decision", "ai-session", "review", "meeting"}
NOTE_STATUSES = {"inbox", "distilled", "archived"}
REQUIRED_NOTE_FIELDS = ("title", "type", "status", "tags", "created", "author", "related")

# SB-0: PARA folders and the two special files of the vault.
PARA_FOLDERS = ("inbox", "projects", "areas", "resources", "archive")
SPECIAL_FILES = ("index.md", "log.md")

# SB-6: note authors are the four members of Team 007.
TEAM_MEMBERS = {
    "Manne Rohith Sai",
    "Sunnapu Suhith Reddy",
    "Rudraraju Dhanush Varma",
    "Abhiram P B S",
}

WIKILINK_RE = re.compile(r"(!?)\[\[([^\]\n]+?)\]\]")
FENCE_RE = re.compile(r"^(```|~~~)")
INLINE_CODE_RE = re.compile(r"`[^`\n]*`")
HEADING_RE = re.compile(r"^(#{1,6})\s+(.+?)\s*#*\s*$")


class FrontMatterError(ValueError):
    pass


def _scalar(value: str):
    value = value.strip()
    if len(value) >= 2 and value[0] == value[-1] and value[0] in "\"'":
        return value[1:-1]
    return value


def parse_front_matter(text: str) -> tuple[dict, str]:
    """Parse the leading '---' YAML block (flat keys, inline or block lists).

    Returns (fields, body). Raises FrontMatterError if the block is missing or malformed.
    """
    lines = text.splitlines()
    if not lines or lines[0].strip() != "---":
        raise FrontMatterError("file does not start with a '---' front-matter block")
    try:
        end = next(i for i in range(1, len(lines)) if lines[i].strip() == "---")
    except StopIteration:
        raise FrontMatterError("front-matter block is not closed with '---'") from None
    fields: dict = {}
    current_list_key = None
    for raw in lines[1:end]:
        if not raw.strip() or raw.lstrip().startswith("#"):
            continue
        if raw.startswith((" ", "\t")):
            # Continuation of the previous key: a block list ('- item') or a nested map ('k: v').
            item = raw.strip()
            if current_list_key is None:
                raise FrontMatterError(f"unexpected indented line: {raw!r}")
            container = fields[current_list_key]
            if item.startswith("- ") and isinstance(container, list):
                container.append(_scalar(item[2:]))
                continue
            if ":" in item and not item.startswith("- "):
                if container == []:
                    container = fields[current_list_key] = {}
                if isinstance(container, dict):
                    k, _, v = item.partition(":")
                    container[k.strip()] = _scalar(v)
                    continue
            raise FrontMatterError(f"cannot mix list items and map entries under '{current_list_key}'")
        key, sep, value = raw.partition(":")
        if not sep:
            raise FrontMatterError(f"line is not 'key: value': {raw!r}")
        key, value = key.strip(), value.strip()
        current_list_key = None
        if value.startswith("[") and value.endswith("]"):
            inner = value[1:-1].strip()
            fields[key] = [_scalar(v) for v in inner.split(",")] if inner else []
        elif value == "":
            fields[key] = []  # becomes a block list or a nested map on the next lines
            current_list_key = key
        else:
            fields[key] = _scalar(value)
    body = "\n".join(lines[end + 1:])
    return fields, body


def strip_code(text: str) -> str:
    """Remove fenced code blocks and inline code so example links are not checked."""
    out, in_fence = [], False
    for line in text.splitlines():
        if FENCE_RE.match(line.strip()):
            in_fence = not in_fence
            out.append("")
            continue
        out.append("" if in_fence else INLINE_CODE_RE.sub("", line))
    return "\n".join(out)


def wikilinks(text: str) -> list[tuple[str, str]]:
    """Return (target, anchor) for every [[target#anchor|alias]] outside code."""
    links = []
    for _bang, inner in WIKILINK_RE.findall(strip_code(text)):
        inner = inner.replace("\\|", "|")  # '\|' is how a link's alias is escaped inside a table
        target = inner.split("|", 1)[0].strip()
        target, _, anchor = target.partition("#")
        links.append((target.strip(), anchor.strip()))
    return links


def headings(text: str) -> set[str]:
    found = set()
    for line in strip_code(text).splitlines():
        m = HEADING_RE.match(line)
        if m:
            found.add(m.group(2).strip())
    return found


def vault_notes(vault: Path) -> dict[str, Path]:
    """Map each note name (file stem, as Obsidian resolves it) to its path.

    Raises ValueError on duplicate names, because such links would be ambiguous.
    """
    notes: dict[str, Path] = {}
    for path in sorted(vault.rglob("*.md")):
        if any(part.startswith(".") for part in path.relative_to(vault).parts):
            continue  # skip .obsidian/ and other hidden folders
        stem = path.stem
        if stem in notes:
            raise ValueError(f"duplicate note name '{stem}': {notes[stem]} and {path}")
        notes[stem] = path
    return notes


def resolve(target: str, notes: dict[str, Path], vault: Path) -> Path | None:
    """Resolve a wikilink target the way Obsidian does (name or vault-relative path)."""
    if target.endswith(".md"):
        target = target[:-3]
    if "/" in target:
        candidate = vault / (target + ".md")
        return candidate if candidate.exists() else None
    return notes.get(target)


def valid_date(value) -> bool:
    if not isinstance(value, str) or not re.fullmatch(r"\d{4}-\d{2}-\d{2}", value):
        return False
    try:
        _dt.date.fromisoformat(value)
        return True
    except ValueError:
        return False


def check_note_front_matter(fields: dict) -> list[str]:
    """Return DR-8 schema violations for one note's front matter."""
    errors = []
    for key in REQUIRED_NOTE_FIELDS:
        if key not in fields:
            errors.append(f"missing front-matter field '{key}'")
    if errors:
        return errors
    if not isinstance(fields["title"], str) or not fields["title"].strip():
        errors.append("'title' must be a non-empty string")
    if fields["type"] not in NOTE_TYPES:
        errors.append(f"'type' must be one of {sorted(NOTE_TYPES)}, got {fields['type']!r}")
    if fields["status"] not in NOTE_STATUSES:
        errors.append(f"'status' must be one of {sorted(NOTE_STATUSES)}, got {fields['status']!r}")
    if not isinstance(fields["tags"], list) or not fields["tags"]:
        errors.append("'tags' must be a non-empty list")
    if not valid_date(fields["created"]):
        errors.append(f"'created' must be a real date YYYY-MM-DD, got {fields['created']!r}")
    if fields["author"] not in TEAM_MEMBERS:
        errors.append(f"'author' must be a Team 007 member {sorted(TEAM_MEMBERS)}, got {fields['author']!r}")
    if not isinstance(fields["related"], list):
        errors.append("'related' must be a list (requirement IDs, issues, PRs)")
    return errors
