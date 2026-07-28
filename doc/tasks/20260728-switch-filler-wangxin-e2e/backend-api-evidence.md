# Backend API Evidence

## Scope

Backend behavior remains authoritative for `openTask` authorization and execution detail snapshot generation. Verification proved the `batchExecutionId + taskId` isolation chain was incomplete in `openOrCreateByContext`, so the backend was patched to carry `reqVO.getTaskId()` through active query, active key and execution creation.

## Contract

## API Contract

- Execution detail response exposes `assistSwitchTasks`.
- `assistSwitchTasks.fillableUsers` comes from active work task `candidateUserSnapshot`.
- `openTask` request accepts optional `assistUserId`; response returns backend-confirmed `assistUserId`.
- `openTask` continues to validate batch status, task status, work task context and permission ability; selected `assistUserId` must exist in the work task candidate snapshot.

## BDD

- BDD: 候选来自执行详情快照 -> Given/When/Then recorded in `execution-log.md`.
- BDD: 后端仍做最终授权 -> Given/When/Then recorded in `execution-log.md`.

## Validation

- `assistUserId` is accepted only when it belongs to the selected work task candidate snapshot.
- Invalid selected `assistUserId` fails with the existing task visibility error; no empty/current-user fallback is added.
- Traditional batch-record execution records are isolated by `batchExecutionId + taskId`, with `MesProBatchRecordExecutionOpenOrCreateByContextReqVO.taskId` persisted to `mes_pro_batch_record_execution.task_id`.

## RED:

- `node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-assist-filler-switch-snapshot-static.spec.cjs` -> FAIL，旧合同未覆盖 `assistUserId` 且弹窗仍依赖全量批次详情。

## GREEN:

- `node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-assist-filler-switch-snapshot-static.spec.cjs` -> PASS。
- `mvn -pl yudao-module-mes -am "-DskipTests" compile` -> PASS。
- `mvn -pl yudao-module-mes "-Dtest=MesProEdhrBatchExecutionServiceTest#openTask_exposesOnlyCurrentUsersAssistRowsFromFrozenResponsibilityScope+openTask_exposesAssistRowsWhenAllRangeScopeCoversSnapshotSourceTable" "-Dsurefire.failIfNoSpecifiedTests=false" "-DforkCount=0" test` -> PASS。
- `mvn -pl yudao-server -am "-DskipTests" package` -> PASS。

## Required Verification

- `node IntRuoyiBackend/yudao-module-mes/src/test/js/mes-edhr-assist-filler-switch-snapshot-static.spec.cjs`
- `mvn -pl yudao-module-mes -am "-DskipTests" compile`

## Verification

- Backend static contract, module compile and targeted JUnit all passed; see `verification-report.md`.

## Blockers

- 无后端验证阻塞。
