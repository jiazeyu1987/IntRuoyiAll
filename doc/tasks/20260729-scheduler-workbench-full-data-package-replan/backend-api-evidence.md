# Backend API Evidence

## Scope

- Service endpoints: `/mes/pro/scheduler-workbench/full-config/export` and `/mes/pro/scheduler-workbench/full-config/import`.
- Service classes: `MesProSchedulerWorkbenchFullConfigPackageServiceImpl` and `MesProSchedulerWorkbenchManualReplanDataPackageServiceImpl`.

## Contract

- Full config export now includes `manualReplanDataPackageBase64` and `manualReplanDataPackage`.
- Manual replan data package version is `scheduler-manual-replan-data.v1`.
- Import fails fast when the full package lacks the manual replan data package or when the manual package lacks required lists.
- Import result returns `replanMasterDataCount`, `replanScheduleOrderDataCount`, and `replanRuntimeDataCount`.

## BDD

- BDD: Full package carries manual replan data -> Given source scheduling data can run manual replan / When exporting and importing full config package / Then manual replan data package is included, imported before route config, and import counts are returned.

## Data Contract

- Master data group: items, processes, routes, route versions/products/processes/flow edges, route flow and schedule configs, production lines, workstations, calendar plans/shifts/holidays, schedule calendar rules, planned/actual capacity, and material stock.
- Schedule order group: production work orders, schedule orders, schedule order process snapshots, and production material lists.
- Runtime group: tasks, task schedule extensions, dependencies, feedbacks, schedule issues, schedule operation logs, and replan explanation snapshots.

## Failure Behavior

- Missing package content uses existing config package error codes.
- Missing row `id` or null row fails fast; no mock/default success path is introduced.
- Import upserts by stable `id` to preserve schedule-order snapshot identities.

## Validation

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProSchedulerWorkbenchFullConfigPackageServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL for missing manual data package behavior.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProSchedulerWorkbenchFullConfigPackageServiceTest,MesProSchedulerWorkbenchManualReplanDataPackageServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 9 tests.

## Verification

- Backend targeted JUnit verification passed after implementation.
- Frontend static contract passed for full config import response counts and success message.

## Blockers

- Full frontend `pnpm ts:check` is blocked by unrelated existing `ExecutionPage.vue(2765,5)` type error.
