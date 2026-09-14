# Frontend Feature Evidence

## Feature Goal

- 选中的工序节点边框显示为紫色。

## Non-Goals

- 不调整工序节点布局、连线、接口或权限。
- 不改变未选中节点的既有绿色边框。

## Requirements And Acceptance

- REQ-1: selected 工序节点必须使用紫色边框。
- AC-1: 静态契约能够检查选中态 class 和紫色边框样式。

## UI Entry Points

- 工艺路线图设计器中的工序节点卡片。

## Owned Files

- `IntRuoyiFronted/src/views/mes/pro/route/RouteFlowGraphDesigner.vue`
- `IntRuoyiFronted/tests/e2e/mes-route-flow-binding-border-static.spec.js`

## API Contracts And Data States

- 不涉及 API 契约变化。

## BDD Scenarios

- BDD: selected process node uses purple border -> Given 工艺路线图中存在可选中的工序节点 / When 某个工序节点处于 selected 状态 / Then 该节点边框显示为紫色且未选中节点保持原样。

## Verification

- RED: `node tests/e2e/mes-route-flow-binding-border-static.spec.js` -> FAIL, selected process node does not yet expose the purple override after binding styles.
- GREEN: `node tests/e2e/mes-route-flow-binding-border-static.spec.js` -> PASS
- Regression: targeted static contract PASS; broader frontend regression was not run because the task is a CSS-only selected-state fix and the workspace contains unrelated in-progress changes.

## Responsive Accessibility States

- 不改变布局与交互，仅修正选中态视觉边框。

## Blockers

- 当前工作区存在大量其他任务改动；本任务只修改工序边框相关文件和任务记录。
