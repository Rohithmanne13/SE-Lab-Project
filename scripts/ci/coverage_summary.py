#!/usr/bin/env python3
"""Coverage report for CI (SRS NFR-MAINT-2: >= 80 % line coverage of the service/domain and persistence layers,
reported by CI).

Reads the JaCoCo CSV that `mvn verify` writes (target/site/jacoco/jacoco.csv), prints the line coverage of every
package as a Markdown table and appends it to the GitHub Actions job summary when $GITHUB_STEP_SUMMARY is set.
Exit status 1 if a gated package (service, domain, persistence) is below the minimum or missing.

Usage: python3 scripts/ci/coverage_summary.py [path/to/jacoco.csv] [--minimum 80]
Standard library only.
"""
from __future__ import annotations

import argparse
import csv
import os
import sys
from pathlib import Path

ROOT = "edu.iiitb.team007.ledger"
GATED = ("service", "domain", "persistence")


def package_lines(csv_path: Path) -> dict[str, tuple[int, int]]:
    """Covered and total lines per package."""
    totals: dict[str, list[int]] = {}
    with csv_path.open(newline="", encoding="utf-8") as fh:
        for row in csv.DictReader(fh):
            t = totals.setdefault(row["PACKAGE"], [0, 0])
            t[0] += int(row["LINE_COVERED"])
            t[1] += int(row["LINE_COVERED"]) + int(row["LINE_MISSED"])
    return {p: (c, n) for p, (c, n) in totals.items()}


def summarise(lines: dict[str, tuple[int, int]], minimum: float) -> tuple[str, list[str]]:
    """The Markdown table and the list of failures."""
    out = ["| Package | Lines covered | Line coverage | Gate |", "|---|---|---|---|"]
    failures = []
    for pkg in sorted(lines):
        covered, total = lines[pkg]
        pct = 100.0 * covered / total if total else 100.0
        short = pkg[len(ROOT) + 1:] if pkg.startswith(ROOT + ".") else pkg
        gated = short in GATED
        ok = pct >= minimum
        gate = (f">= {minimum:.0f} % {'PASS' if ok else 'FAIL'}") if gated else "-"
        if gated and not ok:
            failures.append(f"{short}: {pct:.1f} % < {minimum:.0f} %")
        out.append(f"| `{short}` | {covered}/{total} | {pct:.1f} % | {gate} |")
    for g in GATED:
        if f"{ROOT}.{g}" not in lines:
            failures.append(f"{g}: no coverage data")
    return "\n".join(out), failures


def main(argv: list[str]) -> int:
    ap = argparse.ArgumentParser(description="Print JaCoCo line coverage per package (NFR-MAINT-2).")
    ap.add_argument("csv", nargs="?", default="target/site/jacoco/jacoco.csv")
    ap.add_argument("--minimum", type=float, default=80.0)
    a = ap.parse_args(argv[1:])
    path = Path(a.csv)
    if not path.is_file():
        print(f"FAIL: {path} not found - run `mvn verify` first")
        return 1
    table, failures = summarise(package_lines(path), a.minimum)
    text = "## Line coverage (NFR-MAINT-2)\n\n" + table + "\n"
    print(text)
    summary = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary:
        with open(summary, "a", encoding="utf-8") as fh:
            fh.write(text + "\n")
    for f in failures:
        print(f"FAIL: {f}")
    print("PASS: coverage gate met" if not failures else "FAIL: coverage gate not met")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
