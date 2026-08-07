# Frontend Feature Evidence

## Feature Goal And Non-goals

- Goal: 已完成排产工单的来源生产工单号后显示“(已完成)”。
- Non-goals: 不改变状态、选择、冻结、重排、权限或 API 契约。

## Requirements And Acceptance

- AC-1: `manualFinished=true` 或 `status=3` 的行追加“(已完成)”。
- AC-2: 其他行不追加该标识。

## UI Boundary

- Entry: MES 系统 > 智能排产 > 排产工单 > 排产工单页签。
- Route: `/mes/pro/schedule-order`。
- Component: `IntRuoyiFronted/src/views/mes/pro/scheduleorder/index.vue`。
- Owned test: `IntRuoyiFronted/tests/e2e/mes-schedule-order-completed-source-label-static.spec.cjs`。

## API And State Contract

- 复用 `MesProScheduleOrderVO.erpWorkOrderCode`、`manualFinished` 和 `status`；不新增或修改 API。
- 已完成状态常量沿用页面现有 `SCHEDULE_ORDER_STATUS_FINISHED = 3`。

## BDD Scenarios

- Given 工单已完成；When 渲染来源生产工单号；Then 追加“(已完成)”。
- Given 工单未完成；When 渲染来源生产工单号；Then 保持原文。

## RED

- Command: `node tests/e2e/mes-schedule-order-completed-source-label-static.spec.cjs`
- Expected/actual: FAIL；当前模板只渲染 `row.erpWorkOrderCode`，缺少完成标识 helper 和调用。

## GREEN

- Command: `node tests/e2e/mes-schedule-order-completed-source-label-static.spec.cjs`
- Result: PASS；已完成状态追加“(已完成)”，未完成状态保持原文，且不从进度/数量推断。

## UI State Checks

- Responsive: 文本沿用现有可换行来源工单号单元格，不新增固定宽度控件。
- Accessibility: 保持现有可聚焦链接按钮，标识进入同一可访问名称。
- Loading/empty/error: 不改变现有列表状态。
- Permission: 不改变现有权限指令或操作按钮。

## Real User Path

- Playwright 本机只读验证待执行；要求目标写请求数为 0。

## Blockers And Follow-up Skills

- 当前无 blocker。
