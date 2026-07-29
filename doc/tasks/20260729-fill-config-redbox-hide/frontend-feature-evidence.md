# Frontend Feature Evidence

## Feature Goal

Hide screenshot redbox content in the eDHR fill-config assist-mapping page while keeping required mapping and save capabilities available.

## Non-Goals

- No backend API change.
- No form mapping data model change.
- No fallback or compatibility shim.

## Acceptance

- The top-right redbox action group is not rendered in the dialog header.
- The source-form and assist-preview redbox headings/descriptions are not rendered.
- Source form, assist grid, mapping control panel, and save/reload/close actions remain available.

## UI Entry Point

- `IntRuoyiFronted/src/views/mes/pro/batchrecordformlist/index.vue`
- Dialog: `IntRuoyiFronted/src/views/mes/pro/batchrecordformlist/BatchRecordCellRulesConfirmDialog.vue`

## Owned Files

- `IntRuoyiFronted/src/views/mes/pro/batchrecordformlist/BatchRecordCellRulesConfirmDialog.vue`
- `IntRuoyiFronted/tests/e2e/edhr-fill-config-redbox-hide-static.spec.js`
- `IntRuoyiFronted/tests/e2e/edhr-visual-fill-config-static.spec.js`
- `doc/tasks/20260729-fill-config-redbox-hide/*`
- `docs/frontend-development.md`
- `docs/experience-index.md`

## BDD Scenario

BDD: 隐藏填写配置红框区域 -> Given 用户打开填写配置的辅助表单映射页面 / When 页面渲染原表格、辅助表格和右侧映射控制栏 / Then 顶部右侧操作组、左侧原表单说明栏和中央辅助表单预览说明栏不显示，右侧映射控制栏、辅助表格卡片和必要配置控件仍可见。

## RED:

- `node tests/e2e/edhr-fill-config-redbox-hide-static.spec.js` -> FAIL, expected because the old top action DOM and panel headings still existed.

## GREEN:

- `node tests/e2e/edhr-fill-config-redbox-hide-static.spec.js` -> PASS
- `node tests/e2e/edhr-visual-fill-config-static.spec.js` -> PASS
- `pnpm ts:check` -> PASS

## Verification

- Target static contract, adjacent visual fill config static contract, and TypeScript check all passed.

## UI States

- Loading/error alerts remain unchanged.
- Empty source layout and missing responsibility subject states remain unchanged.
- Close, reload, and save remain visible in the right fixed action area and keep the original handlers.

## Blockers

- Commit/push closeout was not performed in this turn because the workspace already contains unrelated concurrent dirty changes outside this task.
