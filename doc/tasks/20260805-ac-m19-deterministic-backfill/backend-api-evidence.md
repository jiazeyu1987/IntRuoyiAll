# AC-M19 Backend API Evidence

## Scope

Backend service slice for production team leader order-process completion and formal batch-record backfill:

- `MesTeamLeaderOrderProcessCompletionService`
- `MesTeamLeaderBatchRecordBackfillServiceImpl`
- `MesTeamLeaderBatchRecordBackfillCommand`

## API And Data Contract

Backfill command now carries all source process-pool events, confirmed allocations, aggregate hash, and aggregate idempotency key. Completion service locks all confirmed allocations by `workOrderId + routeProcessId + processId`, sorts them deterministically, loads all source events, and passes the complete source set to the formal batch-record writer.

## Auth, Permissions, Validation, Error Behavior

- Auth and controller permissions are unchanged.
- Missing event/allocation/work-order/source context throws `PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED`.
- Missing formal batch-record binding throws `PRO_PROCESS_POOL_BATCH_RECORD_BINDING_REQUIRED`.
- Missing field mapping or missing multi-source strategy throws `PRO_PROCESS_POOL_BATCH_RECORD_FIELD_MAPPING_REQUIRED`.
- Missing source payload/field value throws `PRO_PROCESS_POOL_BATCH_RECORD_SOURCE_VALUE_REQUIRED`.

## Config, Services, Fixtures, Migrations

Requires batch-record execution service, field-audit service, `mes_pro_batch_record_cell_link_rule.aggregation_strategy`, and completion-table aggregate trace columns.

## BDD Scenarios

- `BDD: AC-M19 多事实聚合回填 -> Given 多员工、多设备、多次已确认报工共同完成同一订单工序; When 达到目标量触发正式批记录回填; Then 全部源报工按字段聚合策略写入正式逐工序批记录，且回填幂等键基于同一聚合版本。`
- `BDD: AC-M19 缺聚合策略阻塞 -> Given 多个源报工对同一批记录字段产生多值; When 映射规则没有允许的聚合策略; Then 系统 fail fast，不得取代表事件继续写入。`
- `BDD: AC-M19 聚合源追溯 -> Given 订单工序已完成并回填; When 查询完成状态; Then 持久化聚合源事件、分配、聚合 hash 和幂等键以证明同一聚合版本只写一次。`

## RED

`RED: mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderBatchRecordBackfillServiceTest,MesTeamLeaderOrderProcessCompletionServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, expected reason before fix: service constructor/source command and aggregate field semantics missing.`

## GREEN

`GREEN: mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderBatchRecordBackfillServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> BLOCKED, JVM native memory/page file failure during compile.`

## Contract Verification

`git diff --check` passed for task-owned source, SQL, and test fixture files. Static inspection confirms the AC-M19 path does not introduce `formBindings` or form-slot fallback.

## Observability Touchpoints

Completion rows store `sourceEventIdsJson`, `sourceAllocationIdsJson`, `aggregateHash`, and `backfillIdempotencyKey`. Field-audit writes use `PROCESS_POOL_REPORT_BACKFILL_AGG:<workOrderId>:<routeProcessId>:<processId>:<aggregateHash>`.

## Blockers

Focused Maven test execution remains blocked by local memory pressure and concurrent Maven/Java processes.
