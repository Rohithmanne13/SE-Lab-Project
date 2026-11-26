---
name: brain-refine
description: Refines the team's Obsidian second brain - moves notes from brain/inbox/ into the right PARA folder (projects, areas, resources, archive), marks refined notes status distilled, merges duplicate notes, repairs broken [[wikilinks]], and updates brain/index.md and brain/log.md. Use when the inbox has notes to file, when duplicates or broken links are found, or before a stage submission.
---

# brain-refine

Implements SRS requirement SK-2. Vault rules: [[second-brain-workflow]]. Skill format: [[agent-skills-format]]. PARA meaning: [[para-method]].
Run it from the repository root. It needs Python 3 (standard library only) and changes files only inside `brain/`.

## Inputs

- **Who runs it** (required): the Team 007 member, passed as `--by` - one of `Manne Rohith Sai`, `Sunnapu Suhith Reddy`, `Rudraraju Dhanush Varma`, `Abhiram P B S` (SB-6).
- **Notes to refine** (optional): note names; by default every note in `brain/inbox/`.
- **Target folders** (decided by the agent from the note's content, step 2), e.g. `projects/financial-ledger`, `areas/second-brain`, `resources/sqlite`, `archive`.

## Steps

1. **See the current state:**

   ```bash
   python3 skills/brain-refine/scripts/refine.py status
   ```

2. **Decide a PARA folder for each inbox note** by reading it ([[para-method]]): material for the active project and its deadlines -> `projects/financial-ledger`; an ongoing responsibility without an end date -> `areas/<responsibility>`; reference material on a topic -> `resources/<topic>`; no longer relevant -> `archive`. Use lowercase hyphenated subfolder names and reuse existing ones.
3. **Look for duplicates** - two notes on the same source or the same topic. Keep the better one and merge the other into it (the duplicate goes to `archive/` with a "Merged into" line, and every link to it is rewritten):

   ```bash
   python3 skills/brain-refine/scripts/refine.py --by "<member>" merge <keep> <duplicate>
   ```

4. **Move each note** (it becomes `status: distilled`, or `archived` for `archive`, and its line moves to the right section of `index.md`):

   ```bash
   python3 skills/brain-refine/scripts/refine.py --by "<member>" move <note> <target>
   ```

5. **A note that must stay in the inbox** for now gets a reason, which is shown in `index.md`:

   ```bash
   python3 skills/brain-refine/scripts/refine.py --by "<member>" keep <note> --reason "<why>"
   ```

6. **Repair broken links.** `status` and `check` list them; for a renamed or merged note rewrite the links:

   ```bash
   python3 skills/brain-refine/scripts/refine.py --by "<member>" relink <old-name> <new-name>
   ```

   A link that points at nothing useful is edited by hand in the note.
7. **Verify** - all must pass:

   ```bash
   python3 skills/brain-refine/scripts/refine.py check
   python3 scripts/ci/validate_brain.py
   ```

8. **Report** (format under Outputs), then commit on a `brain/` branch and open a pull request (SB-5).

## Outputs

- Notes moved from `brain/inbox/` into PARA folders with `status: distilled` (or `archived`).
- `brain/index.md` - each note listed under its PARA section (and subsection); "_(none yet)_" placeholders kept up to date.
- `brain/log.md` - one line per change: `- YYYY-MM-DD HH:MM | brain-refine | <what changed> | <member> | <detail>`.
- A report, one line per step, marked PASS or FAIL (UI-4):

  ```text
  brain-refine report
  PASS  status        16 notes in inbox, 0 broken links
  PASS  duplicates    none found
  PASS  moved         16 notes (3 projects, 4 areas, 9 resources)
  PASS  check         inbox empty, 0 broken links
  PASS  validator     validate_brain.py: 0 errors
  ```

## Failure handling

- **The script prints `REJECTED`** (unknown note, invalid target, destination already exists, `--by` not a team member, special note such as `index`, `log` or `srs-requirements`): it wrote nothing. Fix the input it names and run it again.
- **Unsure where a note belongs:** use `keep` with a reason instead of guessing, and raise it in the pull request.
- **`check` or the validator fails after a change:** fix the reported note, or undo everything not yet committed with `git checkout -- brain/ && git clean -fd brain/`, then start again.
- Never delete a note to make a check pass - archive it (`move <note> archive`) so its history stays.

## Evaluation

Run before each stage submission (TQ-7) and record the results in `docs/testing/skill-evaluations.md`:

1. `python3 skills/brain-refine/scripts/refine.py check` - SK-2 acceptance: inbox empty or every remaining note kept with a reason, and zero broken links.
2. `python3 scripts/ci/validate_brain.py` and `python3 scripts/ci/validate_skills.py` - vault schema and this SKILL.md's format.
3. `python3 -m unittest tests.test_brain_refine -v` - moves, keep, merge, relink and every refusal case on a throwaway copy of the vault.
4. A reviewer opens the vault in Obsidian and confirms each refined note sits in a sensible PARA folder.
