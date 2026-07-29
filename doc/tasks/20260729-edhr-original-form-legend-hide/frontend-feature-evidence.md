# Frontend Feature Evidence

## Feature Goal

在 eDHR 执行填写页原表模式隐藏规则类型图例。

## Non-Goals

- 不删除可复用组件的规则图例能力。
- 不隐藏表格单元格右上角的规则类型角标。
- 不改变填写、保存、提交、切换模式或接口行为。

## Acceptance

- `A1`：执行填写页调用模板组件时显式关闭规则图例。
- `A2`：模板组件继续保留图例开关并默认显示，避免影响其他调用页面。
- `A3`：原表模式表格和字段插槽继续存在。

## Entry And Ownership

- Route: `/mes/pro/feedback/edhr-execution/form`
- Page: `IntRuoyiFronted/src/views/mes/pro/edhr/ExecutionPage.vue`
- Shared component: `IntRuoyiFronted/src/views/mes/pro/edhr/components/EdhrExecutionTemplateEditableForm.vue`
- Owned test: `IntRuoyiFronted/tests/e2e/edhr-fill-workspace-original-rule-legend-hidden-static.spec.js`

## API And Data States

- 无 API 契约变化。
- 图例开关仅控制展示，不改变模板布局、单元格规则和草稿数据。

## BDD

- Given 用户进入 eDHR 执行填写页并切换到原表模式
- When 原始批记录表单渲染
- Then 顶部规则类型图例不显示，表格和单元格规则角标继续显示

## TDD Evidence

- RED: `node tests/e2e/edhr-fill-workspace-original-rule-legend-hidden-static.spec.js` -> FAIL，执行填写页未关闭规则图例。
- GREEN: `node tests/e2e/edhr-fill-workspace-original-rule-legend-hidden-static.spec.js` -> PASS。
- REGRESSION: `node tests/e2e/edhr-fill-workspace-static.spec.js`、`node tests/e2e/edhr-batch-template-simulate-red-box-hidden-static.spec.js`、`pnpm ts:check` -> PASS。

## UI Checks

- Responsive: 图例移除后不保留顶部空白区域。
- Accessibility: 不渲染被隐藏图例的 `aria-label`；表格单元格角标的 `aria-label` 保持。
- Loading/Empty/Error: 无行为变化。
- Permission: 无行为变化。

## Blockers

- none
