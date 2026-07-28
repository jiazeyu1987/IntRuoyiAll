# Frontend Feature Evidence

## Goal

优化工艺路线字段明细右侧动态表单整组复制交互，避免来源工序选择后弹层误关闭，并在确认复制成功后关闭弹层。

## Non-Goals

- 不改变表单绑定复制字段。
- 不改变来源工序筛选规则。
- 不改变后端 API、保存流程或权限逻辑。

## Acceptance

- 来源工序下拉选中后，复制 Popover 保持可见并允许继续确认。
- 点击“复制到当前工序”成功后，复制 Popover 显式关闭。
- 复制数据、草稿同步和成功提示保持既有行为。

## UI Entry

- Component: `IntRuoyiFronted/src/views/mes/pro/route/RouteFlowGraphDesigner.vue`
- Entry: 字段明细面板 -> 动态表单列表 -> `data-flow-action="copy-process-form-bindings"`

## BDD Scenarios

- BDD: 复制弹层选择来源不误关闭 -> Given 用户打开动态表单列表的复制弹层 / When 在来源工序下拉中选择一个工序 / Then 弹层保持打开，用户仍可点击“复制到当前工序”。
- BDD: 复制确认后关闭弹层 -> Given 用户已选择来源工序 / When 点击“复制到当前工序”且复制成功 / Then 当前工序表单绑定被替换，草稿同步，成功提示出现，并且复制弹层关闭。

## Implementation

- `el-popover` 增加 `v-model:visible="processFormBindingCopyPopoverVisible"`，将弹层可见性纳入业务状态。
- 来源工序 `el-select` 增加 `:teleported="false"`，避免选项面板点击被外层 Popover 当作外部点击。
- 成功复制后设置 `processFormBindingCopyPopoverVisible.value = false`，同时关闭时清理未确认来源选择。

## Verification

- RED: `node tests/e2e/mes-route-flow-copy-process-form-bindings-static.spec.js` -> FAIL.
- GREEN: `node tests/e2e/mes-route-flow-copy-process-form-bindings-static.spec.js` -> PASS.
- REGRESSION: `node tests/e2e/mes-route-flow-batch-record-detail-slot-filter-static.spec.js` -> PASS.
- REGRESSION: `node tests/e2e/mes-route-flow-binding-border-static.spec.js` -> PASS.
- REGRESSION: `node tests/e2e/mes-route-flow-form-slot-count-badge-static.spec.js` -> PASS.

## Blockers

未运行真实浏览器 E2E；本次只改弹层状态与静态合同，且当前工作区已有大量非本任务脏改动，不适合在本轮直接提交。
