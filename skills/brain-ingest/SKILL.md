---
name: brain-ingest
description: Adds one source (URL, repository file or pasted text) to the team's Obsidian second brain as a research note in brain/inbox/, with DR-8 front matter, a summary and key points taken only from the source, wikilinks to related notes and SRS requirement IDs, and updates brain/index.md and brain/log.md. Use when a team member wants to save research, documentation or course material into the brain.
---

# brain-ingest

Implements SRS requirement SK-1. Vault rules: [[second-brain-workflow]]. Skill format: [[agent-skills-format]].
Run it from the repository root. It needs Python 3 (standard library only) and changes files only inside `brain/`.

## Inputs

- **Source** (required): a URL, a path to a file in this repository, or text pasted by the user.
- **Author** (required): the Team 007 member running the skill - one of `Manne Rohith Sai`, `Sunnapu Suhith Reddy`, `Rudraraju Dhanush Varma`, `Abhiram P B S` (SB-6).
- **Tags** (optional, suggested by the agent if missing): lowercase words, e.g. `sqlite,transactions`.
- **Related** (optional, suggested by the agent if missing): SRS requirement IDs that exist in [[srs-requirements]] (e.g. `FR-2,FR-2.4`), plus GitHub references such as `#12` or `PR #15`. At least one requirement ID is required.

## Steps

1. **Read the whole source.** For a URL, fetch the page; for a file, open it; for pasted text, use it as given. If the source cannot be read completely, stop (see Failure handling).
2. **Check for duplicates.** Search `brain/` for a note on the same source (`grep -ril "<url or file name>" brain/`). If one exists, stop and tell the user its name instead of creating a second note.
3. **Write the note body** to a temporary Markdown file outside `brain/` with exactly these sections:
   - `## Summary` - 2-4 sentences: what the source is (title, author or publisher, version or date) and what it covers.
   - `## Key points` - 4-8 bullet points stating facts from the source.
   - `## Links` - at least one `[[wikilink]]` to a related existing note, each with one line saying how it relates. Our own interpretation must be labelled "Team observation (not from the source)".
4. **Apply the fidelity rule:** every statement in Summary and Key points must be traceable to the source. Do not add facts, numbers, opinions or conclusions that the source does not contain. If unsure, leave it out.
5. **Pick title, slug, tags and related IDs.** The slug is the note name: lowercase words joined by hyphens, unique in the vault. Related requirement IDs must exist in [[srs-requirements]].
6. **Create the note** with the helper script. It adds DR-8 front matter (`type: research`, `status: inbox`), `## Requirements` and `## Source` sections, writes `brain/inbox/<slug>.md`, adds the note under `## Inbox` in `brain/index.md` and appends a dated line to `brain/log.md`:

   ```bash
   python3 skills/brain-ingest/scripts/ingest.py \
     --title "SQLite transactions" --slug sqlite-transactions \
     --source https://www.sqlite.org/lang_transaction.html \
     --author "Manne Rohith Sai" --tags sqlite,transactions \
     --related FR-3,FR-3.1,CON-2 --body /tmp/body.md
   ```

   For pasted text use `--source "pasted text: <short description>, <date received>"`.
7. **Verify** the note with the evaluation script and the vault validator (both must print PASS):

   ```bash
   python3 skills/brain-ingest/scripts/check_note.py brain/inbox/<slug>.md
   python3 scripts/ci/validate_brain.py
   ```

8. **Report** to the user (format under Outputs), then commit the three changed files on a feature branch and open a pull request - brain changes reach `main` only through review (SB-5).

## Outputs

- New file `brain/inbox/<slug>.md` (status `inbox`, until brain-refine files it in Stage 2).
- `brain/index.md` - one new line under `## Inbox`.
- `brain/log.md` - one new line: `- YYYY-MM-DD HH:MM | brain-ingest | created [[<slug>]] in inbox/ | <author> | source: <source>`.
- A short report, one line per step, marked PASS or FAIL (UI-4):

  ```text
  brain-ingest report
  PASS  read source        https://www.sqlite.org/lang_transaction.html
  PASS  duplicate check    no existing note
  PASS  note created       brain/inbox/sqlite-transactions.md
  PASS  index + log        updated
  PASS  evaluation         check_note.py E1-E7, validate_brain.py
  ```

## Failure handling

- **Source unreadable or incomplete** (HTTP error, login wall, page rendered only by JavaScript, unreadable file): do not write a note from memory or partial text. Report FAIL with the reason and ask the user for an accessible copy (e.g. a saved PDF or the pasted text).
- **Duplicate source or slug:** do not overwrite. The script refuses an existing slug; report the existing note.
- **Script rejects the note** (unknown requirement ID, missing section, link to a non-existent note, author not in the team, vault not set up): it writes nothing. Fix the input it names and run it again.
- **Evaluation or validator fails:** fix the note (or delete the new file and revert the `index.md` and `log.md` lines with `git checkout -- brain/index.md brain/log.md`), then re-run step 7. Never commit a note that fails.
- **Secrets:** never paste API keys, tokens or passwords into a note or into the AI tool (AI-4, CON-9).

## Evaluation

Run before each stage submission (TQ-7) and record the results in `docs/testing/skill-evaluations.md`:

1. `python3 skills/brain-ingest/scripts/check_note.py` - checks every inbox note for E1 location, E2 DR-8 front matter, E3 required sections, E4 note and requirement links that resolve, E5 source reference, E6 listed in `index.md`, E7 logged in `log.md`.
2. `python3 scripts/ci/validate_brain.py` and `python3 scripts/ci/validate_skills.py` - vault schema and broken links; this SKILL.md's format.
3. Negative cases - each must be rejected with nothing written: an existing slug, an unknown requirement ID, a body without `## Summary`, an author outside the team.
4. Fidelity review - a reviewer compares at least two notes against their sources and confirms that every key point appears in the source.

Acceptance (SK-1): at least 10 research notes added by this skill in Stage 1, all passing checks 1-2.
