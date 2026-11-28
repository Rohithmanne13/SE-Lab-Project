# Defect log

Every significant defect found while building and testing the product, with its GitHub Issue and the regression
test that failed before the fix and passes after it (SRS TQ-8). Process: `test-plan.md` section 6. Defects of the
brain tooling found in Stages 1 and 2 are listed in `skill-evaluations.md`.

| # | Found | Defect | Issue | Fix | Regression test |
|---|---|---|---|---|---|
| D1 | Stage 3, first run of the API system tests (#39) | The service did not start: the duplicate-key check of the strict JSON settings was given to the Jackson builder as a `StreamReadFeature`, which the builder rejects ("Unknown feature class"), so every Spring context failed | #48 | `JacksonConfig` uses `JsonParser.Feature.STRICT_DUPLICATE_DETECTION` | every system test starts the service; `UsersApiTest.unknownFieldsInvalidJsonAndNonObjectsAreRejected` (duplicate key rejected) |
| D2 | Stage 3, review of the skills against the new start-up rules (#39) | local-check (SK-4) could no longer start the service once API keys became mandatory (SEC-5): its server step would fail on every machine | #49 | `local_check.py` starts the jar with throwaway random keys (`server_env`) | `tests/test_local_check.py` `ServerEnvTest` |
| D3 | Stage 3, first full run with the amount-limit tests (#40) | Tests meant to run on their own database inherited the shared database setting of their base class, which overrode theirs; the limit test pushed the shared INR and USD system accounts to 2^53 - 1 and five unrelated tests failed with AMOUNT_LIMIT_EXCEEDED | #50 | `ApiTestSupport` chooses no database; `IntegrationTestSupport` adds the shared one; isolated tests register their own | `AmountLimitApiTest` and `FaultInjectionTest` `usesItsOwnDatabaseNotTheSharedOne` |
