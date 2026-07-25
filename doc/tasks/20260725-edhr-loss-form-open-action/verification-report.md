# Verification Report

## Summary

修复已完成：批次详情页右侧“损耗单”等路线表单只有在后端策略 `requiredPolicy === 'OPTIONAL'` 时才允许显示和执行跳过表单动作。必填路线表单点击“打开填写”不再因前端把非必填进度口径误当可跳过而调用跳过接口。

## Root Cause

- 前端 `BatchExecutionDetailPage.vue` 的 `isOptionalTask` 使用 `!isRequiredBatchRecordTask(row)` 判断可选表单。
- `isRequiredBatchRecordTask` 原本依赖 `requiredFlag !== false`，而后端跳过接口只接受 `requiredPolicy == OPTIONAL`。
- 当任务不是后端 OPTIONAL 策略时，前端仍可能进入跳过路径，后端正确拒绝并返回“必填路线表单不允许跳过”。

## Changed Files

- `IntRuoyiFronted/src/views/mes/pro/edhr-batch/progress.ts`
- `IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue`
- `IntRuoyiFronted/tests/e2e/edhr-loss-form-open-action-static.spec.js`

## Verification Commands

- `node tests\e2e\edhr-loss-form-open-action-static.spec.js` -> PASS
- `node tests\e2e\edhr-batch-process-companion-forms-static.spec.js` -> PASS
- `node tests\e2e\edhr-pre-release-editable-submit-static.spec.js` -> PASS
- `node tests\e2e\edhr-batch-detail-open-task-worktaskid-static.spec.js` -> PASS
- `rg -n "必填路线表单不允许跳过|edhr-路线表单跳过口径门禁|requiredPolicy OPTIONAL" ...` -> PASS
- `git diff --check -- <task-owned paths>` -> PASS
- `python -X utf8 -c "<read task-owned files as UTF-8>"` -> PASS
- `task_closeout.py --task-id 20260725-edhr-loss-form-open-action --mode preview` -> PASS, no delete/blocked/warnings
- `task_closeout.py --task-id 20260725-edhr-loss-form-open-action --mode apply` -> PASS, no deleted paths

## Experience Gate

- Added `eDHR 路线表单跳过口径门禁` to `docs/e2e-rules.md`.
- Added keyword routing for “必填路线表单不允许跳过 / requiredPolicy OPTIONAL / canSkipOptionalTask” to `docs/experience-index.md`.

## Known Blockers

- Final commit/push is blocked by unrelated concurrent dirty work and existing unpushed local commits on `int_main`.
- Broad static contract `edhr-batch-context-carrier-header-static.spec.js` has an existing assertion mismatch outside this focused fix.
- Broad static contract `edhr-batch-pending-form-entry-static.spec.js` is blocked by a missing historical backend path in the test harness.
