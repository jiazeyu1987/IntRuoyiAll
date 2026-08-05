# Backend API Evidence

## Scope

- `MesProAutoScheduleServiceImpl` 自动排产/重排应用。
- `ScheduleApplier` 应用落库范围与 issue 持久化。
- 排产工单列表响应中的阻断 issue 摘要字段。

## API And Data Contract

- Apply response summary should expose applied, blocked, and skipped work order counts.
- Schedule order rows should expose blocking issue count and latest blocking issue message.
- Per-work-order BLOCKING issues are formal domain records; global/unattributable blockers still fail fast.

## BDD Scenarios

- BDD: Mixed replan scope applies healthy orders -> Given one selected work order is schedulable and another selected work order has an attributable BLOCKING issue, When replan apply runs, Then the healthy work order applies and the blocked work order persists a blocking issue without aborting the full action.
- BDD: All selected orders blocked -> Given all selected work orders carry attributable BLOCKING issues, When replan apply runs, Then no replaceable tasks are deleted and the response reports blocked issues.
- BDD: Blocked orders visible in list -> Given a persisted unresolved BLOCKING issue exists for a work order, When the schedule order list is queried, Then the row returns issue count and latest reason.

## RED

- RED: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProAutoScheduleAlgorithmContractTest#apply_shouldPersistBlockedIssueAndContinueSchedulableWorkOrders" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: an attributable blocked work order caused `PRO_AUTO_SCHEDULE_PREFLIGHT_BLOCKED` and aborted the full apply.

## GREEN

- GREEN: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProAutoScheduleAlgorithmContractTest#apply_shouldPersistBlockedIssueAndContinueSchedulableWorkOrders" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS before final downstream-filter patch, `BUILD SUCCESS`, 1 test, 0 failures.
- Final post-filter re-run on 2026-08-05 reached `yudao-module-mes` `testCompile` and failed before Surefire because unrelated `MesQaInspectionRegulationServiceTest` references missing getters on `MesQaInspectionRegulationProjectStatusRespVO`: `getProductId`, `getConfigured`, `getRegulationId`, `getLifecycleStatus`, and `getRegulationCode`.

## Contract Verification

- Apply summary fields present: `appliedWorkOrderCount`, `blockedWorkOrderCount`, `skippedWorkOrderCount`.
- Schedule order row fields present: `blockingIssueCount`, `latestBlockingIssueMessage`.
- Schedule order controller reads unresolved `BLOCKING` issues by work order and returns count/latest reason.
- Apply path keeps global/unattributable blockers fail-fast and filters blocked work orders from locks, replace/delete scope, preserved relation sync, plan field updates, quantity sync, and EDHR completion commands.

## Validation

- Validation: target backend JUnit passed before the last downstream filter hardening patch.
- Validation: final target backend JUnit re-run is blocked by unrelated MES QA regulation test compilation errors before the target Surefire test can start.

## Observability

- BLOCKING issues must remain queryable through `mes_pro_schedule_issue` and visible in API responses.

## Blockers

- Backend final JUnit re-run after the last downstream-filter patch is blocked by unrelated `MesQaInspectionRegulationServiceTest` compile errors in the shared MES module testCompile phase.
