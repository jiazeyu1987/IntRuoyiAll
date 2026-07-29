# Frontend Feature Evidence

## Feature Goal And Non-Goals

- Goal: 在 eDHR 保存结果弹窗右上角增加可点击关闭按钮。
- Non-goal: 不改变确认按钮、保存/提交 API、订单/工序展示和成功/失败状态展示。

## Requirements And Acceptance

- REQ-1: 保存结果弹窗右上角显示关闭按钮。
- REQ-2: 点击关闭按钮关闭当前结果弹窗，不触发保存或提交请求。

## UI Entry Points And Owned Files

- Entry: eDHR 执行填写页保存草稿或提交执行后的结果弹窗。
- Owned files: 待定位。

## API Contracts And Data States

- 不新增或修改 API。
- 不新增 fallback、默认成功或错误吞并。

## BDD Scenarios

- BDD: 保存结果弹窗关闭按钮 -> Given eDHR 保存结果弹窗显示订单、工序、保存结果和确认按钮, When 用户查看弹窗右上角, Then 红框位置显示可访问的关闭按钮; When 点击关闭按钮, Then 当前结果弹窗关闭且不触发确认按钮以外的新提交或保存行为。

## RED Command And Expected Failure

- Pending.

## GREEN Command And Passing Result

- Pending.

## Responsive Accessibility Loading Empty Error Permission Checks

- Pending.

## E2E Or Component Verification Path

- Pending.

## Blockers And Follow-Up Skills

- Pending.
