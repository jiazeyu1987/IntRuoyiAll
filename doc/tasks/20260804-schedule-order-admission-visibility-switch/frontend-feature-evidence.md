# Frontend Feature Evidence: 同步工单已入池显示开关

## Feature Goal And Non-Goals

- Goal: 在排产工单“同步工单”页签操作区增加“显示已入池订单”开关，控制已加入排产工单池的生产工单是否纳入列表查询。
- Non-goals: 不修改后端接口、不新增 mock 数据、不改变入池提交动作、不调整排产工单主列表。

## Requirements And Acceptance IDs

- REQ-1: 默认隐藏已入池订单。
- REQ-2: 开关打开后显示已入池订单并重新查询第一页。
- REQ-3: 重置按钮恢复默认隐藏已入池订单。

## UI Entry Points, Routes, Components, And Owned Files

- Route: `/mes/pro/schedule-order`
- Component: `IntRuoyiFronted/src/views/mes/pro/scheduleorder/index.vue`
- Test: `IntRuoyiFronted/tests/e2e/mes-schedule-order-admission-show-admitted-switch-static.spec.js`

## API Contracts And Data States

- API wrapper: `MesProScheduleOrderApi.getAdmissionDiff(workOrderAdmissionQueryParams)`
- Query state: 新增前端查询参数表达是否包含 `ALREADY_ADMITTED` 状态。
- Data states: `READY_TO_ADMIT` 可选择入池；`ALREADY_ADMITTED` 仅展示，不可重复选择入池。

## BDD Scenarios

- `BDD: 同步工单默认隐藏已入池订单 -> Given 排产员打开排产工单页面并切换到同步工单页签 / When 页面首次加载同步工单列表 / Then 查询参数默认不包含已加入排产工单池的生产工单，列表聚焦可入池或需处理订单。`
- `BDD: 开关显示已入池订单 -> Given 排产员停留在同步工单页签 / When 打开“显示已入池订单”开关 / Then 页面重新查询第一页，并把已加入排产工单池的生产工单纳入列表展示。`
- `BDD: 重置恢复隐藏已入池订单 -> Given 排产员已打开显示已入池订单开关 / When 点击同步工单页签的重置按钮 / Then 开关恢复关闭状态并重新查询隐藏已入池订单的列表。`

## RED Command And Expected Failure

- Pending command: `node tests/e2e/mes-schedule-order-admission-show-admitted-switch-static.spec.js`

## GREEN Command And Passing Result

- Pending.

## Responsive, Accessibility, Loading, Empty, Error, And Permission Checks

- Responsive: 开关放在同步工单 actions 区，与重置、入池、显示字段同排并允许窄屏换行。
- Accessibility: 开关有清晰文案“显示已入池订单”。
- Loading: 切换开关复用同步工单列表 loading 状态。
- Empty: 查询结果为空时沿用现有列表空态。
- Error: 接口失败继续通过现有错误提示暴露，不吞异常。
- Permission: 开关不提升入池权限；已入池行仍不可选择重复入池。

## E2E Or Component Verification Path

- 使用任务专用静态契约验证 UI、状态和查询参数链路。

## Blockers And Follow-Up Skills

- Pending verification.
