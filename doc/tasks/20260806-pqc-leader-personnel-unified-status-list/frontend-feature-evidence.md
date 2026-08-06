# Frontend Feature Evidence

## Feature Goal

- PQC 组长人员管理删除启用/禁用分组筛选。
- 启用和禁用人员在同一个正式列表中展示。
- 禁用人员姓名红色展示，状态列继续保留文字状态。

## Non-Goals

- 不修改后端接口契约。
- 不修改 PQC 组长模块路由、权限或其它页签行为。
- 不引入前端本地过滤或 mock 数据。

## Requirements And Acceptance

- REQ-1: PQC 人员列表不再按 `enabled` 默认筛选。
- REQ-2: PQC 人员列表请求不再传递 `enabled` 状态筛选参数。
- REQ-3: 禁用人员姓名使用红色样式提示，且仍展示“已禁用”状态文字。

## UI Entry Points

- 入口：PQC 组长工作台的人员管理页签。
- 组件：`IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue`。
- 验证：`IntRuoyiFronted/tests/e2e/pqc-personnel-unified-status-list-static.spec.cjs`。

## API Contracts And Data States

- 使用现有 `getPqcPersonnelList` API。
- 预期调用全量列表，不传 `enabled` 过滤参数。
- `row.enabled === false` 表示禁用状态，驱动姓名红色样式和状态文字。

## BDD Scenarios

- BDD: PQC 人员启停状态统一列表 -> Given PQC 人员中同时存在启用与禁用人员 / When PQC 组长打开人员管理列表 / Then 页面不再提供启用或禁用分组筛选，并在同一列表加载全部人员。
- BDD: 禁用 PQC 人员姓名红色提示 -> Given 某个 PQC 人员为禁用状态 / When 该人员展示在人员管理列表 / Then 人员姓名以红色显示，且状态列仍显示“已禁用”。

## RED

- pending

## GREEN

- pending

## Responsive Accessibility Loading Empty Error Permission

- 响应式：不改变现有表格布局和页签结构。
- 可访问性：状态列保留“已启用/已禁用”文字，颜色仅作额外提示。
- Loading/Empty/Error：沿用现有 PQC 人员列表加载、空态和错误处理。
- 权限：不改变路由、菜单或按钮权限。

## E2E Or Component Verification Path

- 当前先以静态合同覆盖用户可见结构、请求参数和样式契约。
- 如需真实页面 E2E，应使用已确认运行态、PQC 组长账号和测试租户，不以 API-only 替代。

## Blockers And Follow-Up Skills

- pending
