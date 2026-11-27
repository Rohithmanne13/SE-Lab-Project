# ADR-0001: Implementation stack - Java 21, Spring Boot, Maven, SQLite JDBC

## Status

Accepted (2026-11-23)

## Context

The SRS leaves the implementation language and framework open and asks us to record the choice as an ADR in Stage 2 (CON-6): one mainstream language with a maintained HTTP framework, test framework, linter and SQLite driver. Other requirements constrain the choice:

- SQLite 3.35.0 or later is the system of record, in-process, file database for normal runs and an isolated database for tests (CON-1, OE-2, TQ-5).
- Money-moving work runs in explicit write transactions started with `BEGIN IMMEDIATE`, with per-connection PRAGMAs (SI-2, SI-3, CON-2) - we need direct control over SQL and transactions.
- RESTful JSON under `/api/v1`, documented in OpenAPI 3.x (CON-4, SE-7); errors as RFC 9457 problem documents (FR-0.3).
- Layered code with provider interfaces that tests can substitute (CON-5, SE-6); validation of every parameter (FR-0.2, SEC-1).
- Exact, pinned dependency versions (SEC-9); builds and tests on Windows, macOS and Linux (OE-1, NFR-PORT-1); CI on GitHub Actions (PR-5).
- Every member must be able to explain the code (AI-2), so team familiarity matters.

## Decision

| Concern | Choice | Version (pinned) |
|---|---|---|
| Language and runtime | Java (LTS) | 21 |
| HTTP framework | Spring Boot (Spring Web MVC, embedded Tomcat) | 3.5.8 (via `spring-boot-starter-parent`) |
| Data access | Spring JDBC (`JdbcTemplate`) with hand-written SQL - **no JPA/Hibernate** | managed by Spring Boot 3.5.8 |
| SQLite driver | `org.xerial:sqlite-jdbc` | 3.49.1.0 |
| Validation | Jakarta Bean Validation (`spring-boot-starter-validation`) | managed by Spring Boot 3.5.8 |
| Error responses | Spring `ProblemDetail` (RFC 9457) - see ADR-0004 | built in |
| Tests | JUnit 5 + Spring Boot Test (`spring-boot-starter-test`) | managed by Spring Boot 3.5.8 |
| Lint | Checkstyle via `maven-checkstyle-plugin`, fails the build on any violation | plugin 3.6.0, Checkstyle 10.23.1 |
| Build tool | Apache Maven through the Maven Wrapper (`mvnw`, `mvnw.cmd`); `sh ./mvnw -B verify` = lint, compile, test, package | Maven 3.9.11 (wrapper 3.3.2) |
| Runnable artifact | Spring Boot fat jar `target/financial-ledger-api.jar` | - |

- Package root `edu.iiitb.team007.ledger`, layered as `api` -> `service` -> `persistence`, with `domain` and `support` (provider interfaces); see `docs/design/architecture.md`.
- Every dependency and plugin version is written exactly in `pom.xml` or inherited from the one pinned Spring Boot parent; no ranges, no `LATEST`/`RELEASE` (SEC-9).
- CI's "build, lint, test" job runs `mvn -B -ntp verify` on Java 21 (Temurin) for every push and pull request (PR-5); locally the same build runs as `sh ./mvnw -B verify` (Windows: `mvnw.cmd -B verify`).

## Alternatives considered

- **Javalin (Java)** - small and explicit, but validation, RFC 9457 errors and OpenAPI generation would be hand-built; less familiar to the team.
- **Spring Data JPA / Hibernate** - hides SQL and transaction boundaries; SQLite support is unofficial, and we must control `BEGIN IMMEDIATE`, triggers and `CHECK` constraints exactly (ADR-0002, ADR-0003).
- **Python (FastAPI) or Node.js (Express)** - viable, but the team's existing tooling and experience are in Java and Maven, and Java's typed `long` arithmetic keeps money in integer minor units by construction (CON-3, TQ-6).
- **Gradle** - equivalent capability; Maven chosen for its simpler, declarative build and the team's familiarity.

## Consequences

- Spring Boot gives validation, problem details, test slices and OpenAPI tooling (springdoc, Stage 3) without custom plumbing; startup stays well inside the 15 s budget of NFR-PERF-5.
- Hand-written SQL means more code to review, but every statement is visible and parameterised (SEC-3).
- Team members need only JDK 21 locally; the Maven Wrapper downloads the pinned Maven 3.9.11 on first use (README). The `local-check` skill (SK-4) verifies the setup on each machine.
- The CI "build, lint, test" job, which skipped until a build existed, now builds for real.

## Sources

- [[sqlite-transactions]] - transaction types and `BEGIN IMMEDIATE`, which require explicit transaction control from the data-access layer.
- [[rfc-9457-problem-details]] - the error format Spring's `ProblemDetail` implements.
- [[openapi-specification-3-1]] - the API description format the framework's tooling must produce (SE-7).
- [[srs-requirements]] - CON-6 and the constraints listed above.
