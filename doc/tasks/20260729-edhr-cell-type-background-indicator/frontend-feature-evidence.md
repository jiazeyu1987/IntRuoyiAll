# Frontend Feature Evidence

## Feature Goal

在 eDHR 执行填写页用类型背景色替代可填写单元格右上角类型小标识。

## Non-Goals

- 不删除共享模板组件的小标识能力。
- 不改变模板规则来源、字段值、保存、提交或签名行为。
- 不修改辅助填写模式或后端 API。

## Acceptance

- `A1`：执行填写页原表模式显式使用背景色类型展示。
- `A2`：背景色模式下两种模板视口都不渲染单元格类型小标识。
- `A3`：七种类型均有明确、可区分的背景色，字段内容仍保留。
- `A4`：共享组件默认仍使用小标识模式。

## Entry And Ownership

- Route: `/mes/pro/feedback/edhr-execution/form`
- Page: `IntRuoyiFronted/src/views/mes/pro/edhr/ExecutionPage.vue`
- Shared component: `IntRuoyiFronted/src/views/mes/pro/edhr/components/EdhrExecutionTemplateEditableForm.vue`
- Owned test: `IntRuoyiFronted/tests/e2e/edhr-fill-workspace-cell-type-background-static.spec.js`

## API And Data States

- 无 API 契约变化。
- 类型仍来自正式 `cellRules` / `TemplateEditableCellContext`，仅改变视觉投影。

## BDD

- BDD:
- Given 用户进入 eDHR 执行填写页原表模式
- When 模板单元格渲染
- Then 右上角类型 item 不显示，单元格按类型显示不同背景色，字段内容和控件保留

## TDD Evidence

- RED: `node tests/e2e/edhr-fill-workspace-cell-type-background-static.spec.js` -> FAIL，执行页未启用类型背景色模式。
- GREEN: `node tests/e2e/edhr-fill-workspace-cell-type-background-static.spec.js` -> PASS。
- REGRESSION: `node tests/e2e/edhr-fill-workspace-original-rule-legend-hidden-static.spec.js`、`node tests/e2e/edhr-fill-workspace-static.spec.js`、`node tests/e2e/edhr-batch-template-simulate-red-box-hidden-static.spec.js`、`node tests/e2e/edhr-assist-fill-mode-static.spec.js` -> PASS。

## Verification

- 类型背景色专用合同和相邻填写页合同通过。
- `pnpm ts:check` 复跑失败于未涉及本任务的 `ActionFormPanel.vue:257`，保留为既有 blocker。

## Verification

- pending

## UI Checks

- Responsive: 背景色应用于单元格本身，不增加宽度、不遮挡控件。
- Accessibility: 不依赖小标识文本布局；输入控件、字段标签和真实校验信息保持。
- Loading/Empty/Error: 无行为变化。
- Permission: 无行为变化。

## Blockers

- none
