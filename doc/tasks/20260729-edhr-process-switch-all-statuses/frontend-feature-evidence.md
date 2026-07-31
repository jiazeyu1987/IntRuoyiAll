# Frontend Feature Evidence

## Feature Goal

辅助填写页“工序”切换弹窗展示当前批次全部普通工序，复用批次执行工序状态背景，并保留正式 openTask 门禁。

## Non-goals

- 不修改后端接口契约。
- 不放宽 `OPEN_FORM`、接管、跳过、保存或提交权限。
- 不改变“填写人”切换的 FormCenter 槽位导航逻辑。

## Requirements

- R1: 工序切换列表来自当前批次详情的全部普通工序任务，不能只过滤可打开任务。
- R2: 列表按工序分组展示，不把同一工序下多个表单任务误当多个工序。
- R3: 状态展示覆盖待打开、草稿、已提交、已驳回、需返工、填写完成、已跳过、阻塞。
- R4: 选中、未开始、进行中/已开始、已完成状态视觉与批次执行页面保持一致。
- R5: 实际进入填写仍必须走正式 openTask；非可打开但有执行记录时只读打开；尚未产生执行记录/工作任务的工序进入批次详情并按 `batchTaskId` 选中。

## UI Entry Points

- Route/component: `src/views/mes/pro/edhr/ExecutionPage.vue`
- Dialog menu: `data-assist-switch-menu="process"`
- Existing reference: `src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue`

## API Contracts

- Read current batch tasks: `getEdhrBatchExecution(batchExecutionId)`
- Open editable task: `openEdhrBatchTask({ batchExecutionId, taskId, workTaskId, assistUserId })`
- Read-only execution view: `/mes/pro/feedback/edhr-execution/form?id=<executionId>&batchExecutionId=<batchExecutionId>&batchTaskId=<taskId>`

## BDD Scenarios

- See `execution-log.md`.

## RED Command

- `node tests/e2e/edhr-assist-process-switch-all-statuses-static.spec.js` -> FAIL before implementation, because grouped `AssistProcessSwitchItem` did not exist and process switch still filtered to openable tasks.

## GREEN Command

- `node tests/e2e/edhr-assist-process-switch-all-statuses-static.spec.js` -> PASS after implementation draft.
- `node tests/e2e/edhr-switch-filler-selectability-static.spec.js` -> PASS.
- `node tests/e2e/edhr-switch-filler-formcenter-slot-static.spec.js` -> PASS.
- `node tests/e2e/edhr-assist-fill-mode-static.spec.js` -> PASS.
- `pnpm ts:check` -> PASS.

## Verification Notes

- The specific user-reported error path is covered by `navigateToAssistBatchProcessOverview(row, batchExecutionId)`.
- No `OPEN_FORM` permission is granted for non-openable tasks; the route changes to the read-only batch detail process context.
