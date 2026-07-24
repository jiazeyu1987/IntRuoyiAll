# Task: MES admin-api missing routes

## Goal

Fix the frontend error that reports missing request addresses for MES item page and home statistics endpoints:

- `admin-api/mes/md/item/page`
- `admin-api/mes/home-statistics/production-trend`
- `admin-api/mes/home-statistics/work-order-status`
- `admin-api/mes/home-statistics/summary`

## Milestones

- [x] M1: Previous backend task state checked before starting this task.
- [x] M2: Task document and BDD/TDD evidence log created before production code changes.
- [x] M3: Reproduce the missing-route behavior and identify the exact frontend/backend route mismatch.
- [x] M4: Add failing regression coverage for the missing expected endpoints.
- [x] M5: Implement the minimal fix without fallback or silent downgrade.
- [x] M6: Run targeted verification, update evidence, and commit only current task changes.

## Expected Verification

- `mvn -pl yudao-server -am "-Dtest=MesModuleEnablementTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `mvn -pl yudao-server -am "-Dmaven.test.skip=true" "-Dspring-boot.repackage.skip=true" package`

## Current Status

Completed. Verification passed and the final commit must include only the MES task documents, MES enablement regression test, and MES POM enablement hunks.

## Root-Cause Hypothesis

The running `yudao-server` did not register MES controllers because `yudao-module-mes` was commented out in the root Maven reactor and in `yudao-server/pom.xml`.

## Evidence

- Reproduction completed with `mvn "-Dtest=MesModuleEnablementTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` from `yudao-module-mes`; the test failed because the root `pom.xml` and `yudao-server/pom.xml` do not include MES.
- Runtime logs show `NoResourceFoundException` for `/admin-api/mes/md/item/page` and the MES home-statistics endpoints.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesModuleEnablementTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS.
- GREEN: `mvn -pl yudao-server -am "-DskipTests" package` -> PASS, produced executable `yudao-server.jar` with `BOOT-INF/lib/yudao-module-mes-2026.04-SNAPSHOT.jar`.
- Runtime verification: direct requests to `http://localhost:48081/admin-api/mes/md/item/page?pageNo=1&pageSize=10`, `.../summary`, `.../work-order-status`, and `.../production-trend?days=7` returned HTTP 200.

## Blockers

- None known yet.
