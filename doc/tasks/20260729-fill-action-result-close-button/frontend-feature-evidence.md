# Frontend Feature Evidence

## Feature Goal And Non-Goals

- Goal: 在 eDHR 保存结果弹窗右上角增加可点击关闭按钮。
- Non-goal: 不改变确认按钮、保存/提交 API、订单/工序展示和成功/失败状态展示。

## Requirements And Acceptance

- REQ-1: 保存结果弹窗右上角显示关闭按钮。
- REQ-2: 点击关闭按钮关闭当前结果弹窗，不触发保存或提交请求。

## UI Entry Points And Owned Files

- Entry: eDHR 执行填写页保存草稿或提交执行后的结果弹窗。
- Owned files:
  - `IntRuoyiFronted/src/views/mes/pro/edhr/ExecutionPage.vue`
  - `IntRuoyiFronted/tests/e2e/edhr-fill-workspace-action-result-dialog-static.spec.js`

## API Contracts And Data States

- 不新增或修改 API。
- 不新增 fallback、默认成功或错误吞并。

## BDD Scenarios

- BDD: 保存结果弹窗关闭按钮 -> Given eDHR 保存结果弹窗显示订单、工序、保存结果和确认按钮, When 用户查看弹窗右上角, Then 红框位置显示可访问的关闭按钮; When 点击关闭按钮, Then 当前结果弹窗关闭且不触发确认按钮以外的新提交或保存行为。

## RED Command And Expected Failure

- RED: `node tests/e2e/edhr-fill-workspace-action-result-dialog-static.spec.js` -> FAIL，合同缺少 `edhr-fill-workspace__result-close`、`closeFillActionResultDialog` 和右上角受控关闭按钮。

## GREEN Command And Passing Result

- GREEN: `node tests/e2e/edhr-fill-workspace-action-result-dialog-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-execution-fill-workspace-submit-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。

## Responsive Accessibility Loading Empty Error Permission Checks

- Responsive: 关闭按钮使用绝对定位 `top: 12px; right: 12px`，弹窗继续使用 `max-width: calc(100vw - 32px)`。
- Accessibility: 关闭按钮提供 `aria-label="关闭结果弹窗"`，并保留 `focus-visible` 样式。
- Loading/empty: 不改变保存、提交 loading 或空状态逻辑。
- Error: 提交失败结果弹窗展示真实失败原因，不吞异常，不替换为默认成功。
- Permission: 不新增权限入口；结果弹窗仅在既有保存/提交链路之后显示。

## E2E Or Component Verification Path

- Static component contract: `tests/e2e/edhr-fill-workspace-action-result-dialog-static.spec.js`。
- Adjacent regression: `tests/e2e/edhr-execution-fill-workspace-submit-static.spec.js`。
- Type verification: `pnpm ts:check`。

## Blockers And Follow-Up Skills

- Blockers: 无。
- Follow-up skills: `task-closeout-cleanup` 用于收尾 preview/apply；`project-experience-consolidation` 复核无新增长期经验条目需求。
