---
name: local-check
description: Confirms the Financial Ledger API project works on a team member's machine - builds it with Maven, runs the automated test suite, starts the local server on a throwaway database, calls the read-only GET /health endpoint, and reports PASS or FAIL for each step. Use after cloning, after pulling changes, before opening a pull request, or when someone says the project does not work on their machine.
---

# local-check

Implements SRS requirement SK-4 (the common skill of the course SDLC document, section 3). Skill format: [[agent-skills-format]]. What it builds and starts: `docs/design/architecture.md`, decided in ADR-0001.
Run it from the repository root. It needs JDK 21 and Python 3 (standard library only); Maven comes from the repository's Maven Wrapper; it never touches the real database (NFR-SAFE-5).

## Inputs

- **Who runs it** (optional, recommended): the Team 007 member, passed as `--by` - one of `Manne Rohith Sai`, `Sunnapu Suhith Reddy`, `Rudraraju Dhanush Varma`, `Abhiram P B S`. With `--by` the run is logged in `brain/log.md` (SB-0).
- **Maven command** (optional): `--mvn <command>`; by default the Maven Wrapper (`sh ./mvnw`, on Windows `mvnw.cmd`), or `mvn` if the wrapper is missing.
- **Port** (optional): `--port <n>`; by default a free local port is chosen.

## Steps

1. **Check the tools** - `java -version` must show 21. If not, see Failure handling.
2. **Run the check:**

   ```bash
   python3 skills/local-check/scripts/local_check.py --by "<member>"
   ```

   The script performs the five steps of SK-4 in order:
   1. **build** - `sh ./mvnw -B -DskipTests package` (Checkstyle lint included) produces `target/financial-ledger-api.jar`;
   2. **tests** - `sh ./mvnw -B test` and `python3 -m unittest discover -s tests`;
   3. **server** - starts the jar on `127.0.0.1` and a free port with a fresh temporary database file, and waits up to 60 s until it answers;
   4. **fetch** - `GET /health` must return HTTP 200 with `"status":"ok"` and `"database":"ok"` (FR-6.4);
   5. **report** - one line per step, PASS / FAIL / SKIP; the server is always stopped.
3. **Read the report.** The exit status is 0 only when every step passed. For a FAIL, open the log file named on the `report` line.
4. **If you ran it with `--by`**, commit the new `brain/log.md` line with your next brain pull request (SB-5).

## Outputs

- A report on the terminal (UI-4), e.g.:

  ```text
  local-check report
  PASS  build    exit 0
  PASS  tests    Maven tests exit 0; Python tests exit 0
  PASS  server   http://127.0.0.1:37825/health answering after 4.4 s
  PASS  fetch    HTTP 200 {"status":"ok","database":"ok","version":"0.2.0"}
  PASS  report   details in /tmp/local-check-xxxx/local-check.log
  PASS: 5/5 steps passed on Linux
  ```

- Exit status 0 (all passed) or 1 (any step failed); 2 if `--by` names someone outside the team.
- With `--by`: one line in `brain/log.md`: `- YYYY-MM-DD HH:MM | local-check | PASS 5/5 steps | <member> | <OS>, Python <version>`.
- A full log of every command's output in a temporary folder (path in the report).

## Failure handling

- **build FAIL, "Maven not found" or "JAVA_HOME" errors:** install JDK 21 (Ubuntu/WSL: `sudo apt install -y openjdk-21-jdk`); the first wrapper run also needs internet access to download Maven 3.9.11. `--mvn <command>` uses another Maven.
- **build FAIL with Checkstyle violations:** the log lists file, line and rule; fix them and run again - the CI job fails on the same rules.
- **tests FAIL:** the log names the failing test; run `sh ./mvnw -B test` or `python3 -m unittest discover -s tests -v` to see it directly. Do not push until it passes.
- **server FAIL "no answer within 60 s" or "exited":** read the end of the log (port in use, wrong Java version, cannot create the database folder); retry with `--port <another>` or `--timeout 120` on a slow machine.
- **fetch FAIL with HTTP 503 "degraded":** the service started but cannot use its database - check that the temporary folder is writable.
- Steps after a failed build are reported as SKIP, never as PASS; the server is stopped even when a step fails.

## Evaluation

Run before each stage submission (TQ-7) and record the results in `docs/testing/skill-evaluations.md`:

1. `python3 skills/local-check/scripts/local_check.py --by "<member>"` on every member's machine - all 5 steps PASS (SK-4 acceptance; NFR-PORT-1 across the members' operating systems).
2. `python3 -m unittest tests.test_local_check -v` - health check against healthy, degraded and missing servers, report format, missing Maven and unknown member.
3. `python3 scripts/ci/validate_skills.py` - this SKILL.md's format.
4. Live run in the Stage 4 review.
