# Bug Regression Evidence

## Regression

右侧当前工序表单卡片原先把 `detail?.batchExecutionCode` 作为每张卡片顶部主标题，多个不同表单任务会重复显示同一个 `EDHRB-...`，造成用户误以为是重复表单。

## Root Cause

卡片模板把批次上下文字段放进了任务卡片标题位置，而不是使用任务自身的表单名称。

## Fix

- 删除卡片内 `edhr-batch-detail__rail-execution-code` 批次号标题。
- 新增 `resolveTaskCardDisplayName(row)`，基础名称来自 `resolveTaskDisplayName(row)`。
- 当 `row.status === EDHR_BATCH_TASK_STATUS_DRAFT` 且名称有效时返回 `${name}*`。

## Verification

- RED: `node tests/e2e/edhr-batch-card-title-draft-marker-static.spec.js` -> FAIL，旧卡片仍存在批次号标题。
- GREEN: `node tests/e2e/edhr-batch-card-title-draft-marker-static.spec.js` -> PASS。
- GREEN: 相邻右侧栏静态合同和 `pnpm ts:check` 均通过。

## Remaining Risk

真实页面只读 Playwright 验证因缺少 `EDHR_COMPANION_E2E_PASSWORD` 与 `PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH` 未执行。
