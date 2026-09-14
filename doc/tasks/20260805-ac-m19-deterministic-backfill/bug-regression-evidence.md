# AC-M19 Bug Regression Evidence

## Bug Summary

AC-M19 formal batch-record backfill previously used only the representative completion event and allocation. That could drop earlier confirmed reports for the same work order, route process, and process, and did not persist deterministic aggregate source or idempotency evidence.

## Expected Behavior

- All confirmed reports for the same `workOrderId + routeProcessId + processId` are aggregated into the formal batch record.
- Formal writes use the route-process `BATCH_RECORD` binding and cell-link rules only.
- Multi-source fields require an explicit aggregation strategy.
- Completion rows persist source event ids, source allocation ids, aggregate hash, and aggregate idempotency key.

## Reproduction Command

`mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderBatchRecordBackfillServiceTest,MesTeamLeaderOrderProcessCompletionServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`

## Root Cause

`MesTeamLeaderBatchRecordBackfillServiceImpl` parsed `command.event.rawPayload` directly and generated a representative-event idempotency key. `MesTeamLeaderOrderProcessCompletionService` passed only the representative allocation to backfill instead of the full locked confirmed allocation set.

## Regression Tests

- `MesTeamLeaderBatchRecordBackfillServiceTest.shouldAggregateAllConfirmedReportsIntoFormalBatchRecordWithConfiguredStrategies`
- `MesTeamLeaderBatchRecordBackfillServiceTest.shouldBlockMultiSourceBackfillWhenAggregationStrategyIsMissing`
- `MesTeamLeaderOrderProcessCompletionServiceTest.shouldCompleteOrderProcessAndTriggerBackfillWhenCumulativeQuantityReachesTarget`

## RED

`RED: mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderBatchRecordBackfillServiceTest,MesTeamLeaderOrderProcessCompletionServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, expected reason before fix: representative event path drops prior confirmed report data and lacks aggregate source/idempotency trace.`

## GREEN

`GREEN: mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderBatchRecordBackfillServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> BLOCKED, local JVM failed during module compilation because Windows page file/native memory was insufficient.`

## Verification

- `git diff --check -- <task-owned files>` passed.
- Focused Maven execution is blocked by local JVM native memory/page-file pressure.
- Static inspection confirms the repaired path requires formal `BATCH_RECORD` binding and aggregate source/idempotency trace.

## Risk And Regression Scope

Scope is limited to AC-M19 formal batch-record backfill, order-process completion trace fields, and schema contracts for aggregation strategy. No fallback, mock success, or silent downgrade was added.

## Blockers And Follow-Up

Maven verification is blocked by local memory pressure and concurrent Java/Maven processes in the shared workspace. Retry focused tests after the background processes finish or after increasing available page file/memory.
