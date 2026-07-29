# Verification Report

## Scope

修复执行页顶部“工序”切换：选择当前批次任意普通工序时保留在 `/mes/pro/feedback/edhr-execution/form`，对无 `executionId`/无工作任务的工序使用正式 `task/preview` 只读查看，不再跳转到流程详情页。

## Commands

- `node tests/e2e/edhr-assist-process-switch-all-statuses-static.spec.js` -> PASS
- `node tests/e2e/edhr-assist-fill-mode-static.spec.js` -> PASS
- `node tests/e2e/edhr-switch-filler-selectability-static.spec.js` -> PASS
- `node tests/e2e/edhr-switch-filler-formcenter-slot-static.spec.js` -> PASS
- `pnpm ts:check` -> PASS

## Evidence

- 同页切换：`navigateToAssistBatchTaskPreview(row, batchExecutionId)` pushes `/mes/pro/feedback/edhr-execution/form` with `batchTaskPreview: '1'`.
- 正式预览数据：`loadAssistBatchTaskPreviewExecution` reads both `getEdhrBatchExecution` and `getEdhrBatchTaskPreview`.
- 只读边界：preview route does not set `workTaskId`, so `isReadonly` remains true and save/submit actions are not enabled.
- 路由刷新：execution-page watcher now includes `batchExecutionId`, `batchTaskId`, and `batchTaskPreview`, so switching process on the same page reloads the selected process context.

## Result

PASS. Implementation and focused verification are complete; closeout/commit/push remains.
