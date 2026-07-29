# Frontend Feature Evidence

## Feature Goal And Non-Goals

- Goal: 在电子签名弹窗右上角增加可点击关闭按钮。
- Non-goal: 不改变确认按钮、签名提交 API、姓名展示和电子签名输入校验。

## Requirements And Acceptance

- REQ-1: 电子签名弹窗右上角显示关闭按钮。
- REQ-2: 点击关闭按钮复用既有弹窗关闭事件，不触发确认签名。

## UI Entry Points And Owned Files

- Entry: 触发电子签名确认的现有页面弹窗。
- Owned files:
  - `IntRuoyiFronted/src/views/mes/pro/edhr/ExecutionPage.vue`
  - `IntRuoyiFronted/tests/e2e/edhr-execution-fill-workspace-submit-static.spec.js`

## API Contracts And Data States

- 不新增或修改 API。
- 不新增 fallback、默认成功或错误吞并。

## BDD Scenarios

- BDD: 电子签名弹窗关闭按钮 -> Given 电子签名弹窗显示姓名、电子签名输入框和确认按钮, When 用户查看弹窗右上角, Then 红框位置显示可访问的关闭按钮; When 点击关闭按钮, Then 弹窗通过既有关闭事件关闭且不触发确认签名。

## RED Command And Expected Failure

- Pending.

## GREEN Command And Passing Result

- Pending.

## Responsive Accessibility Loading Empty Error Permission Checks

- Accessibility: 关闭按钮必须有 `aria-label="关闭电子签名弹窗"` 并使用 `ep:close` 图标。
- Responsive: 关闭按钮固定在弹框右上角，弹框仍使用 `max-width: calc(100vw - 32px)`。
- Loading: 不改变提交 loading 状态或提交请求。
- Empty/Error/Permission: 不新增 API、不改变错误提示或权限门禁。

## E2E Or Component Verification Path

- Pending.

## Blockers And Follow-Up Skills

- Pending.
