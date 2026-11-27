#!/usr/bin/env python3
"""brain-query evaluation: does the search find the right notes for a fixed question set? (SK-3, TQ-7)

For every question in skills/brain-query/evals/questions.json, at least one expected note must be
ranked in the top 3 results and every expected note must appear in the top 5.

Usage: python3 skills/brain-query/scripts/eval_query.py [--vault brain]
Exit status 0 = every question passed, 1 = at least one failed.
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import query  # noqa: E402


def main(argv: list[str]) -> int:
    ap = argparse.ArgumentParser(description="Evaluate brain-query retrieval (SK-3).")
    ap.add_argument("--vault", default="brain")
    ap.add_argument("--questions", default=str(HERE.parent / "evals" / "questions.json"))
    a = ap.parse_args(argv[1:])
    cases = json.loads(Path(a.questions).read_text(encoding="utf-8"))
    failed = 0
    for c in cases:
        names = [r["name"] for r in query.search(Path(a.vault), c["question"], top=5)]
        top3_ok = any(e in names[:3] for e in c["expected"])
        top5_ok = all(e in names for e in c["expected"])
        ok = top3_ok and top5_ok
        failed += not ok
        print(f"{'PASS' if ok else 'FAIL'}  {c['id']}  expected {c['expected']}  got {names}")
    print(f"{'PASS' if not failed else 'FAIL'}: {len(cases) - failed}/{len(cases)} questions found their expected notes")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
