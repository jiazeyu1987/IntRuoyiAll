# Bug Regression Evidence

## Bug Summary

手动重排成功后，来源生产工单号为 `881MO093613`、`881MO093615` 的两个排产工单产品编号没有变橙色。

## Expected Behavior

成功应用重排后，页面必须从本次真实预览结果解析参与工单，并更新 `lastReplanParticipatingScheduleOrderIds`。产品编号 class 必须仅对本次参与重排的工单返回 `schedule-order-pool__product-code--scheduled`。

## Reproduction

- 真实页面：排产工单页签 -> 勾选 `881MO093613`、`881MO093615` -> 手动重排 -> 开始重排 -> 确认应用重排。
- 回归测试：`node tests/e2e/mes-replan-product-code-current-selection-static.spec.js`。

## Root Cause

`updateLastReplanParticipatingScheduleOrders()` 已定义，但成功应用重排的 `confirmApplyReplanStartChoice()` 未调用它；参与集合保持为空，产品编号因此始终使用未排产黑色样式。

## Regression Test

- Existing regression test: `IntRuoyiFronted/tests/e2e/mes-replan-product-code-current-selection-static.spec.js`.
- Required assertion: the successful apply flow calls `updateLastReplanParticipatingScheduleOrders(freshPreview)` only after `replanApply`.

## RED

- `node tests/e2e/mes-replan-product-code-current-selection-static.spec.js` -> FAIL，断言“成功应用重排后必须调用 `updateLastReplanParticipatingScheduleOrders(freshPreview)`”未满足。

## GREEN

- 待执行。

## Risk And Scope

- 仅更新手动重排成功后的前端展示状态和甘特图刷新事件携带的参与工单 ID。
- 不改变后端排产算法、重排请求、预览、应用、数据表或错误处理。

## Blockers

- 暂无。
