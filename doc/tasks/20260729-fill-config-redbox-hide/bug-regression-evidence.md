# Bug Regression Evidence

## Bug Summary

“填写配置 / 辅助表单映射”页面截图红框内的顶部操作组、左侧原表单说明栏和中央辅助表单预览说明栏仍显示。

## Expected Behavior

红框说明和顶部操作区域不显示；原表格、辅助表格、右侧映射控制栏和保存/重读/关闭能力仍可用。

## Reproduction

- Path: 批记录表单列表 -> 目标表单 -> 填写配置 -> 辅助表单映射。
- Deterministic check: `node tests/e2e/edhr-fill-config-redbox-hide-static.spec.js`

## Root Cause

`BatchRecordCellRulesConfirmDialog.vue` 在辅助映射页持续渲染 `data-fill-config-actions="primary"` 顶部操作组，以及左侧和中间栏的 `batch-record-cell-rules-editor__panel-head` 说明标题。

## Regression Test

- Added `IntRuoyiFronted/tests/e2e/edhr-fill-config-redbox-hide-static.spec.js`.
- Updated `IntRuoyiFronted/tests/e2e/edhr-visual-fill-config-static.spec.js` to match the new hidden-header contract.

## RED:

- `node tests/e2e/edhr-fill-config-redbox-hide-static.spec.js` -> FAIL, expected because the right-side fixed action area was missing and the old redbox DOM still existed.

## GREEN:

- `node tests/e2e/edhr-fill-config-redbox-hide-static.spec.js` -> PASS
- `node tests/e2e/edhr-visual-fill-config-static.spec.js` -> PASS
- `pnpm ts:check` -> PASS

## Verification

- Target static contract, adjacent visual fill config static contract, and TypeScript check all passed.

## Risk And Scope

- Scope is limited to the MES batch-record fill-config dialog.
- No API contract, save payload, mapping logic, or error handling was changed.
- No fallback, mock success, or swallowed exception was introduced.

## Blockers

- Commit/push closeout was not performed in this turn because the workspace already contains unrelated concurrent dirty changes outside this task.
