---
name: brain-query
description: Answers a question, or drafts a document such as an SRS section or an ADR, using only the content of the team's Obsidian second brain in brain/, citing every note it uses as a [[wikilink]] and saying plainly when the vault does not contain the answer; logs every query in brain/log.md. Use when someone asks what the team knows about a topic, needs sources for a design decision, or wants a draft grounded in the vault.
---

# brain-query

Implements SRS requirement SK-3. Vault rules: [[second-brain-workflow]]. Skill format: [[agent-skills-format]].
Run it from the repository root. It needs Python 3 (standard library only); it only appends to `brain/log.md`.

## Inputs

- **Question or drafting task** (required), e.g. "Which HTTP status code do we return when an idempotency key is reused?" or "Draft the Context section of an ADR on transaction isolation".
- **Who asks** (required for the log): the Team 007 member, passed as `--by` - one of `Manne Rohith Sai`, `Sunnapu Suhith Reddy`, `Rudraraju Dhanush Varma`, `Abhiram P B S`.

## Steps

1. **Search the vault:**

   ```bash
   python3 skills/brain-query/scripts/query.py search "<question>"
   ```

   It ranks the best-matching notes (never `index.md` or `log.md`) and shows the lines that matched. If the ranking misses an obvious note, search again with other words from the question.
2. **Read the top notes in full** - at least the first three that look relevant. A requirement is looked up in `[[srs-requirements]]` by its ID.
3. **Write the answer only from what those notes say.** Every statement carries the note it comes from as a `[[wikilink]]` (a requirement as `[[srs-requirements#FR-2.4|FR-2.4]]`). Do not add facts from memory or from the web; label anything that is the team's own reasoning as such.
4. **If the notes do not contain the answer**, say so plainly - "The vault does not contain the answer to this question" - and name the closest notes found. Suggest a source to add with the brain-ingest skill.
5. **For a drafting task**, build the draft section by section from the notes, citing them the same way; a draft ADR must cite at least one distilled note (SB-3).
6. **Log the query** (exactly one of `--cited` or `--no-answer`):

   ```bash
   python3 skills/brain-query/scripts/query.py log "<question>" --by "<member>" --cited note-a,note-b
   python3 skills/brain-query/scripts/query.py log "<question>" --by "<member>" --no-answer
   ```

7. **Report** to the person who asked (format under Outputs). A log entry is committed with the next brain pull request (SB-5).

## Outputs

- The answer or draft, with a `[[wikilink]]` to every note used, or the plain statement that the vault does not contain the answer.
- `brain/log.md` - one line per query: `- YYYY-MM-DD HH:MM | brain-query | answered "<question>" | <member> | cited [[a]], [[b]]` (or `the vault does not contain the answer`).
- A short report, one line per step, marked PASS or FAIL (UI-4):

  ```text
  brain-query report
  PASS  search      5 notes ranked; top: [[idempotency-key-header-draft]]
  PASS  read        3 notes read in full
  PASS  answer      2 notes cited
  PASS  log         brain/log.md updated
  ```

## Failure handling

- **Search returns nothing or nothing relevant:** answer "The vault does not contain the answer", log it with `--no-answer`, and propose a source to ingest. Never fill the gap from memory.
- **Notes disagree with each other or with the SRS:** report both, citing each; the SRS (`[[srs-requirements]]`) wins for requirements.
- **`log` prints `REJECTED`** (unknown member, cited note does not exist, both or neither of `--cited` / `--no-answer`): nothing was written; fix the named input and run it again.
- Never edit notes from this skill - changes go through brain-ingest or brain-refine.

## Evaluation

Run before each stage submission (TQ-7) and record the results in `docs/testing/skill-evaluations.md`:

1. `python3 skills/brain-query/scripts/eval_query.py` - for each of the 15 questions in `skills/brain-query/evals/questions.json`, an expected note must rank in the top 3 and every expected note in the top 5.
2. `python3 -m unittest tests.test_brain_query -v` - ranking, the `log` command and its refusals on a throwaway copy of the vault.
3. `python3 scripts/ci/validate_skills.py` - this SKILL.md's format.
4. A reviewer asks three of the evaluation questions through an AI agent following this skill and confirms each answer cites the expected notes and adds nothing that is not in them (SK-3 acceptance).
