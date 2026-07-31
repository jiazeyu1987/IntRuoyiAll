# Frontend Feature Evidence

## Scope

- Page: `src/views/mes/pro/scheduler-workbench/index.vue`.
- API wrapper: `src/api/mes/pro/schedulerWorkbench/index.ts`.

## Contract

- `SchedulerWorkbenchFullConfigImportRespVO` includes `replanMasterDataCount`, `replanScheduleOrderDataCount`, and `replanRuntimeDataCount`.
- Full config import success message displays user-role replay counts plus the three manual replan data counts.
- Existing long import timeout contract remains unchanged.

## Acceptance

- Full data package import feedback must visibly report manual replan master data, schedule order data, and runtime data counts.

## BDD

- BDD: Full package import displays manual replan counts -> Given the full config import endpoint returns manual replan count fields / When the user imports the full data package / Then the success message shows the three count groups.

## Verification

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProSchedulerWorkbenchFullConfigPackageServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL before backend implementation because the required count-producing data package was absent.
- GREEN: `node tests/e2e/mes-scheduler-workbench-import-timeout-static.spec.js` -> PASS.
- BLOCKED: `pnpm ts:check` -> FAIL in unrelated existing file `src/views/mes/pro/edhr/ExecutionPage.vue(2765,5)`; this task did not edit that file.

## Blockers

- Full `pnpm ts:check` remains blocked by unrelated existing `ExecutionPage.vue(2765,5)` type error.
