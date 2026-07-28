# Verification Report

## Summary

修复已完成：批次详情页右侧“损耗单”等路线表单只有在后端策略 `requiredPolicy === 'OPTIONAL'` 时才允许显示和执行跳过表单动作。必填路线表单有填写权限时显示并执行“打开填写”；仅有查看权限时显示“查看表单”，打开只读表单面板，不调用填写、跳过或 legacy 批记录预览接口。

## Root Cause

- 前端 `BatchExecutionDetailPage.vue` 的 `isOptionalTask` 使用 `!isRequiredBatchRecordTask(row)` 判断可选表单。
- `isRequiredBatchRecordTask` 原本依赖 `requiredFlag !== false`，而后端跳过接口只接受 `requiredPolicy == OPTIONAL`。
- 当任务不是后端 OPTIONAL 策略时，前端仍可能进入跳过路径，后端正确拒绝并返回“必填路线表单不允许跳过”。
- 当前账号无损耗单填写权限但有查看权限时，原页面没有独立只读主动作，容易继续以“打开填写”表达可写意图。
- 追加排查发现：动态表单卡片被选择时，`selectProcessTask` 在没有已匹配执行记录时仍调用 `getEdhrBatchTaskPreview`，而该接口只适用于 legacy 批记录表单；动态表单缺少 `batchRecordReportId` 时会把表单中心只读查看误报成“必填路线表单不允许跳过”。

## Permission Behavior

- 有填写权限：卡片主动作显示 `打开填写`，调用正式 `openEdhrBatchTask` 后打开可编辑动态表单。
- 无填写权限但有查看权限：卡片主动作显示 `查看表单`，使用任务已有表单实例上下文打开只读抽屉，不调用 `openEdhrBatchTask`，不调用跳过接口。
- 动态表单选中态：不请求 `/task/preview`；只切换卡片选中态，由右侧 `查看表单` 打开只读抽屉。
- 无查看所需表单上下文：卡片不开放主动作，继续展示门禁原因或只读预览错误。

## Changed Files

- `IntRuoyiFronted/src/views/mes/pro/edhr-batch/progress.ts`
- `IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue`
- `IntRuoyiFronted/src/views/form-center/business-action/ActionFormPanel.vue`
- `IntRuoyiFronted/tests/e2e/edhr-loss-form-open-action-static.spec.js`
- `IntRuoyiFronted/tests/e2e/edhr-loss-form-open-action-real.e2e.js`

## Verification Commands

- `node tests\e2e\edhr-loss-form-open-action-static.spec.js` -> PASS
- `node --check tests\e2e\edhr-loss-form-open-action-real.e2e.js` -> PASS
- `node tests\e2e\edhr-loss-form-open-action-real.e2e.js` -> PASS
- `node tests\e2e\edhr-batch-process-companion-forms-static.spec.js` -> PASS
- `node tests\e2e\edhr-pre-release-editable-submit-static.spec.js` -> PASS
- `node tests\e2e\edhr-batch-detail-open-task-worktaskid-static.spec.js` -> PASS
- `rg -n "必填路线表单不允许跳过|edhr-路线表单跳过口径门禁|requiredPolicy OPTIONAL" ...` -> PASS
- `git diff --check -- <task-owned paths>` -> PASS
- `python -X utf8 -c "<read task-owned files as UTF-8>"` -> PASS
- `task_closeout.py --task-id 20260725-edhr-loss-form-open-action --mode preview` -> PASS, no delete/blocked/warnings
- `task_closeout.py --task-id 20260725-edhr-loss-form-open-action --mode apply` -> PASS, no deleted paths
- `task_closeout.py --task-id 20260725-edhr-loss-form-open-action --mode preview` -> PASS after view-only extension, no delete/blocked/warnings
- `task_closeout.py --task-id 20260725-edhr-loss-form-open-action --mode apply` -> PASS after view-only extension, no deleted paths
- `task_closeout.py --task-id 20260725-edhr-loss-form-open-action --mode preview` -> PASS after real E2E, kept JSON + screenshot evidence, no delete/blocked/warnings
- `task_closeout.py --task-id 20260725-edhr-loss-form-open-action --mode apply` -> PASS after real E2E, no deleted paths
- `task_closeout.py --task-id 20260725-edhr-loss-form-open-action --mode preview` -> PASS after dynamic preview fix, kept bug evidence + JSON + screenshot, no delete/blocked/warnings
- `task_closeout.py --task-id 20260725-edhr-loss-form-open-action --mode apply` -> PASS after dynamic preview fix, no deleted paths

## Real E2E Evidence

- Runtime: local frontend `http://localhost:8081` and local backend `http://127.0.0.1:48081` with health `UP`.
- Login preflight: `芋道源码/admin` passed through `scripts/preflight/login-preflight.mjs`; password was not recorded.
- Latest target: `batchExecutionId=900000000846`, batch code `E2E-FULL-1785024829153`, loss form task `taskId=6557`, process `粗洗工序`, `requiredPolicy=REQUIRED`, `allowedActions=[]`, `formCenterInstanceId=312`, `formTemplateId=25`.
- UI assertions: loss form card primary action is `查看表单`; clicking it opens `查看表单：损耗单`; readonly notice is visible; `解析 / 创建 / 保存草稿 / 提交 / 重提 / 放弃` buttons are all disabled.
- Network assertions: no `/mes/pro/edhr-batch-execution/task/preview`, no `/mes/pro/edhr-batch-execution/task/open`, no `/mes/pro/edhr-batch-execution/task/special-node/skip`, and no MES/FormCenter write request occurred.
- Error assertion: `skipErrorCount=0`，页面未显示“必填路线表单不允许跳过”。
- Evidence files: `doc/tasks/20260725-edhr-loss-form-open-action/real-e2e-output/readonly-loss-form-card-result.json` and `doc/tasks/20260725-edhr-loss-form-open-action/real-e2e-output/readonly-loss-form-card.png`.

## Experience Gate

- Added `eDHR 路线表单跳过口径门禁` to `docs/e2e-rules.md`.
- Added keyword routing for “必填路线表单不允许跳过 / requiredPolicy OPTIONAL / canSkipOptionalTask” to `docs/experience-index.md`.
- Extended the same gate for view-only route form cards: no `OPEN_FORM` must verify `查看表单` through a real card click, with readonly drawer actions disabled and no write requests.
- Extended the same gate for dynamic form selection: `formTemplateId/formCenterInstanceId` tasks must not call legacy `/task/preview`; E2E should assert `previewRequests=[]` and no “必填路线表单不允许跳过” alert.

## Known Blockers

- Final commit/push is blocked by unrelated concurrent dirty work and existing unpushed local commits on `int_main`.
- Broad static contract `edhr-batch-context-carrier-header-static.spec.js` has an existing assertion mismatch outside this focused fix.
- Broad static contract `edhr-batch-pending-form-entry-static.spec.js` is blocked by a missing historical backend path in the test harness.
