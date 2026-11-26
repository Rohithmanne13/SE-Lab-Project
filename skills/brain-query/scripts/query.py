#!/usr/bin/env python3
"""brain-query helper: find the vault notes that answer a question, and log each query (SRS SK-3).

Sub-commands (run from the repository root):
  search "<question>" [--top N]     rank the notes that best match the question and show the
                                    matching lines, so the agent can read them and answer
                                    ONLY from their content
  log "<question>" --by "<member>" (--cited a,b | --no-answer)
                                    append the query to brain/log.md with the notes the answer
                                    cites, or record that the vault did not contain the answer

Ranking: words of the question (minus common words) are matched against each note's title (x3),
tags (x3), headings (x2) and body (x1), weighted so that rare words count more. index.md and
log.md are never returned. Standard library only.
"""
from __future__ import annotations

import argparse
import datetime as dt
import math
import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(REPO / "scripts" / "ci"))
import brainlib as bl  # noqa: E402

STOP = set("""a an and are as at be by can do does for from has have how i if in into is it its
of on or our should so that the their them then there these this to was we what when where which
who why will with you your about after before between than our us also any each per not no yes
use used using via vs""".split())
WORD = re.compile(r"[a-z0-9][a-z0-9\-\.]*[a-z0-9]|[a-z0-9]")


def words(text: str) -> list[str]:
    out = []
    for w in WORD.findall(text.lower()):
        w = w.strip(".-")
        if len(w) < 2 or w in STOP:
            continue
        out.append(w)
        if "-" in w:  # also match the parts of hyphenated words (e.g. write-ahead -> write, ahead)
            out += [p for p in w.split("-") if len(p) > 1 and p not in STOP]
    return out


def load_notes(vault: Path) -> list[dict]:
    notes = []
    for name, path in bl.vault_notes(vault).items():
        rel = path.relative_to(vault).as_posix()
        if rel in bl.SPECIAL_FILES:
            continue
        text = path.read_text(encoding="utf-8")
        try:
            fields, body = bl.parse_front_matter(text)
        except bl.FrontMatterError:
            fields, body = {}, text
        tags = fields.get("tags") if isinstance(fields.get("tags"), list) else []
        notes.append({
            "name": name, "path": rel, "title": str(fields.get("title", name)),
            "status": fields.get("status", "?"), "tags": tags, "body": body,
            "heads": " ".join(bl.headings(body)),
        })
    return notes


def search(vault: Path, question: str, top: int = 5) -> list[dict]:
    q = list(dict.fromkeys(words(question)))
    notes = load_notes(vault)
    if not q or not notes:
        return []
    fields_of = []
    for n in notes:
        fields_of.append({
            "title": words(n["title"] + " " + n["name"].replace("-", " ")),
            "tags": words(" ".join(n["tags"])),
            "heads": words(n["heads"]),
            "body": words(n["body"]),
        })
    df = {t: sum(1 for f in fields_of if t in f["body"] or t in f["title"] or t in f["tags"]) for t in q}
    total = len(notes)
    results = []
    for n, f in zip(notes, fields_of):
        score = 0.0
        hit_terms = []
        for t in q:
            if df[t] == 0:
                continue
            idf = math.log(1 + total / df[t])
            tf = 3 * f["title"].count(t) + 3 * f["tags"].count(t) + 2 * f["heads"].count(t) + min(f["body"].count(t), 5)
            if tf:
                score += idf * (1 + math.log(tf))
                hit_terms.append(t)
        if score > 0:
            # prefer notes that cover more of the question
            score *= (0.5 + len(hit_terms) / len(q))
            lines = [ln.strip() for ln in n["body"].splitlines() if ln.strip() and not ln.startswith("#")]
            snippets = sorted(lines, key=lambda ln: -sum(1 for t in hit_terms if t in ln.lower()))[:2]
            results.append({**n, "score": round(score, 2), "matched": hit_terms, "snippets": snippets})
    results.sort(key=lambda r: (-r["score"], r["name"]))
    return results[:top]


def cmd_log(vault: Path, question: str, by: str, cited: list[str], no_answer: bool) -> str:
    if by not in bl.TEAM_MEMBERS:
        raise ValueError(f"--by must be one of {sorted(bl.TEAM_MEMBERS)}")
    if not question.strip():
        raise ValueError("the question must not be empty")
    if no_answer == bool(cited):
        raise ValueError("give either --cited <notes> or --no-answer")
    notes = bl.vault_notes(vault)
    missing = [c for c in cited if c not in notes]
    if missing:
        raise ValueError(f"cited note(s) do not exist: {missing}")
    q = question.strip().replace("|", "/").replace("\n", " ")
    detail = "the vault does not contain the answer" if no_answer else "cited " + ", ".join(f"[[{c}]]" for c in cited)
    stamp = dt.datetime.now().strftime("%Y-%m-%d %H:%M")
    with (vault / "log.md").open("a", encoding="utf-8") as fh:
        fh.write(f"- {stamp} | brain-query | answered \"{q}\" | {by} | {detail}\n")
    return f"logged query ({detail})"


def main(argv: list[str]) -> int:
    ap = argparse.ArgumentParser(description="Search the brain/ vault and log queries (SK-3).")
    ap.add_argument("--vault", default="brain")
    sub = ap.add_subparsers(dest="cmd", required=True)
    s = sub.add_parser("search"); s.add_argument("question"); s.add_argument("--top", type=int, default=5)
    lg = sub.add_parser("log"); lg.add_argument("question"); lg.add_argument("--by", required=True)
    g = lg.add_mutually_exclusive_group(required=True)
    g.add_argument("--cited", help="comma-separated note names the answer cites")
    g.add_argument("--no-answer", action="store_true", help="the vault did not contain the answer")
    a = ap.parse_args(argv[1:])
    vault = Path(a.vault)
    if not (vault / "index.md").is_file() or not (vault / "log.md").is_file():
        print(f"brain-query: REJECTED - {vault}/ is not a vault (index.md / log.md missing)", file=sys.stderr)
        return 1
    if a.cmd == "search":
        results = search(vault, a.question, a.top)
        if not results:
            print("No matching notes - the vault does not contain an answer to this question.")
            return 0
        for i, r in enumerate(results, 1):
            print(f"{i}. [[{r['name']}]]  ({r['path']}, status {r['status']}, score {r['score']})")
            print(f"   {r['title']}")
            for snip in r["snippets"]:
                print(f"   > {snip[:200]}")
        return 0
    try:
        cited = [c.strip() for c in (a.cited or "").split(",") if c.strip()]
        print("brain-query: " + cmd_log(vault, a.question, a.by.strip(), cited, a.no_answer))
        return 0
    except ValueError as exc:
        print(f"brain-query: REJECTED - {exc} (nothing was written)", file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main(sys.argv))
