# Bug Regression Evidence

## Bug Summary And Expected Behavior

- Bug: “单元格规则”弹窗顶部红框内的报表名称、规则数量、待确认数量、后端待确认数量和规则编辑模式提示仍显示。
- Expected: 红框内顶部汇总栏不显示；左侧只读表单预览、右侧规则编辑面板和保存规则动作继续保留。

## Reproduction

- Path: 打开批记录表单列表中的“单元格规则”弹窗。
- Deterministic command: `node tests/e2e/batch-record-cell-rule-summary-hidden-static.spec.js`。

## Root Cause

- `BatchRecordCellRulesConfirmDialog.vue` 模板仍渲染 `batch-record-cell-rules-editor__summary` 区域，并保留对应 CSS 与计算状态。

## Regression Test

- Added `IntRuoyiFronted/tests/e2e/batch-record-cell-rule-summary-hidden-static.spec.js`。
- The test asserts the summary class and visible summary tokens are absent, while workspace, preview, side panel and save action remain present.

## RED

- `node tests/e2e/batch-record-cell-rule-summary-hidden-static.spec.js` -> FAIL。
- Expected reason: component still contained `batch-record-cell-rules-editor__summary`.

## GREEN

- `node tests/e2e/batch-record-cell-rule-summary-hidden-static.spec.js` -> PASS。
- `node tests/e2e/batch-record-cell-rule-default-fullscreen-static.spec.js` -> PASS。
- `node tests/e2e/edhr-cell-rule-type-background-colors-static.spec.js` -> PASS。
- `pnpm ts:check` -> PASS。

## Risk And Regression Scope

- Scope is limited to `BatchRecordCellRulesConfirmDialog.vue` header rendering and the new focused static contract.
- Risk is low because the fix removes a standalone summary block and related unused state without changing API calls, rule editing, preview cells, default fullscreen, or save flow.

## Blockers And Follow-Up

- No implementation blocker.
- Browser E2E was not run because this is a static display removal in an existing dialog and the targeted static contracts plus type check cover the changed behavior.
