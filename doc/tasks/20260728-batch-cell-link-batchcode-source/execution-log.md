# Execution Log

## User Intent

- 用户报告创建批次执行时报错：`批记录单元格链接自动落库缺少来源值：executionId=32，ruleId=16，sourceField=batchCode，targetCell=4:1`。
- 期望修复批记录单元格链接自动落库时 `batchCode` 来源值缺失的根因。

## Preflight

- Skill: `bug-regression-fix-loop`，按复现、RED、最小修复、GREEN、回归验证处理。
- Read: `docs/task-closeout-rules.md`。
- Read: `docs/powershell-encoding.md`。
- Read: `docs/powershell-memory.md`。
- Read: `docs/backend-development.md`。
- Read: `docs/database-rules.md`。
- Git baseline: existing dirty workspace committed before current task changes.
- Baseline commit: `5c4aab0b chore: baseline existing dirty workspace`.
- Baseline files:
  - `doc/tasks/20260728-start-local-frontend-backend-runtime/execution-log.md`
  - `doc/tasks/20260728-start-local-frontend-backend-runtime/task.md`
  - `doc/tasks/20260728-start-local-frontend-backend-runtime/verification-report.md`
  - `docs/experience-index.md`
  - `docs/local-runtime.md`

## BDD

- BDD: 创建批次执行自动落库批号链接 -> Given 已启用批记录单元格链接规则 `PRODUCTION_WORK_ORDER.batchCode` 到目标单元格，且批记录执行上下文存在创建时解析后的批号；When 创建批次执行并打开/创建批记录执行记录；Then 后端从正式执行上下文读取 `batchCode` 并通过字段审计链写入 `cell_values_json`，目标已有人工值不得被覆盖，来源真实缺失时 fail fast。

## TDD Evidence

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellLinkServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, new test `getPrefill_resolvesProductionBatchCodeFromExecutionContextWhenWorkOrderBatchCodeEmpty` expected one prefill but got zero because `PRODUCTION_WORK_ORDER.batchCode` read `MesProWorkOrderDO.batchCode` only.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellLinkServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 6 tests.
- REGRESSION: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellLinkServiceImplTest,MesProBatchRecordCellLinkAutoPersistServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 10 tests.

## Root Cause

- `PRODUCTION_WORK_ORDER.batchCode` 的运行态预填只从生产工单主表 `MesProWorkOrderDO.batchCode` 取值。eDHR 批次执行创建时，`MesProBatchRecordExecutionServiceImpl.resolveBatchCode` 已把创建入参批号解析并写入 `MesProBatchRecordExecutionDO.batchCode`，但单元格链接服务没有读取该执行上下文批号；当生产工单主表批号为空而本次批次执行批号存在时，`getPrefill` 返回 `SOURCE_VALUE_MISSING`，随后自动落库服务 fail-fast 抛出用户看到的缺少来源值错误。

## Milestone Updates

- 2026-07-28: Task documentation created after dirty-worktree baseline commit.
- 2026-07-28: Added regression test for execution-context batch code when work order `batchCode` is empty.
- 2026-07-28: Fixed `MesProBatchRecordCellLinkServiceImpl.resolveProductionWorkOrderFieldValue` so `sourceField=batchCode` uses the already resolved batch record execution context batch code.
- 2026-07-28: Targeted and adjacent regression Maven tests passed.
- 2026-07-28: Project experience consolidated into `docs/backend-development.md#批记录单元格链接预填落库边界`; `docs/experience-index.md` updated with `mes_pro_batch_record_execution.batch_code` and 执行上下文批号 keywords.
- 2026-07-28: Cleanup preview -> ready, keep task core records and bug evidence, delete none, blocked none, warnings none.
- 2026-07-28: Cleanup apply -> applied, deleted none.
- 2026-07-28: Implementation commit `b6f5d35f fix: resolve batch cell link batch code source`.
- 2026-07-28: Closeout commit `49d0f5ef docs: close batch cell link batch code task`.
- 2026-07-28: Push `git push origin int_main` -> PASS, remote advanced `5946a5b6..49d0f5ef`; `git rev-list --left-right --count origin/int_main...HEAD` -> `0 0`.
- 2026-07-28: Observed unrelated deletion `doc/tasks/20260728-edhr-batch-record-design-docs/output/电子批记录系统设计说明书.docx`; left untouched and excluded from current task scope.

## Blockers

- None currently.
