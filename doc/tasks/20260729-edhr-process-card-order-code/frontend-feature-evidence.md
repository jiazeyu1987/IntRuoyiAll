# Frontend Feature Evidence

## Feature Goal

调整 eDHR “切换工序”弹框卡片：更高卡片、更大字体、隐藏卡片明细，并在弹框顶部显示订单号。

## Non-Goals

- 不修改后端接口或工序候选数据来源。
- 不改变点击工序后的正式 `openTask` / 只读 / 批次详情跳转链路。
- 不引入 fallback、mock 数据、默认成功或吞异常。

## Requirements

- REQ-1: 工序卡片高度增加，字体变大。
- REQ-2: 卡片内原二级说明文字不显示。
- REQ-3: 弹框顶部中间位置展示订单号。
- REQ-4: 状态标签和工序切换点击行为保持。

## Acceptance

- AC-1: 工序卡片最小高度大于上一版 64px。
- AC-2: 工序卡片主标题字体和状态标签字体显式增大。
- AC-3: 工序卡片模板不再渲染 `resolveAssistProcessSwitchItemSecondaryLabel(item)`。
- AC-4: process 切换菜单头部渲染订单号字段。

## UI Entry Points

- eDHR 填写页顶部“工序”信息卡中的“切换”按钮。

## Owned Files

- pending

## API Contracts And Data States

- 不新增或修改 API。
- 订单号优先来自当前执行页已加载上下文。

## BDD Scenarios

- BDD: 工序卡片放大展示 -> Given 用户打开“切换工序”弹框, When 工序卡片渲染, Then 卡片高度更高、工序名和状态字体更大。
- BDD: 隐藏卡片明细说明 -> Given 工序卡片包含状态下方的序号/表单项/直接前置说明, When 弹框显示, Then 这些红框位置明细不再显示。
- BDD: 弹框顶部展示订单号 -> Given 当前填写页有订单号上下文, When 用户打开“切换工序”弹框, Then 弹框顶部黄框位置显示订单号。

## Verification Plan

- RED: 新增聚焦静态合同，证明当前卡片仍显示二级说明且未展示订单号。
- GREEN: 修改组件后运行同一聚焦静态合同。
- REGRESSION: 运行相邻工序切换 grid 和 all-status 静态合同，必要时运行 `pnpm ts:check`。

## Responsive / Accessibility / States

- pending

## Blockers

- pending
