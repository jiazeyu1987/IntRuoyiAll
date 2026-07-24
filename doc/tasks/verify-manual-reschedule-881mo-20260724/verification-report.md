# Verification Report

## Objective

在本机 `http://127.0.0.1:8081`，使用用户授权的 `芋道源码/admin` 身份标签，执行来源生产工单号为 `881MO093613`、`881MO093615` 的两个排产工单真实页面手动重排，并核验四项目标。

## Status

FAIL

## Evidence

- Playwright 真实页面路径：排产工单页签 -> 勾选两个目标工单 -> 手动重排 -> 开始重排 -> 选择 `2026-07-25` -> 确认应用重排。
- 重排成功截图已在验证时采集；截图中的成功提示为新增任务 `136` 个、删除任务 `136` 个、保留任务 `7` 个。该临时证据按任务收尾规则清理，关键字段已记录在本报告和执行日志。
- 页面成功提示：新增任务 `136` 个、删除任务 `136` 个、保留任务 `7` 个。
- 最近成功排产接口：`operationType=REPLAN_APPLY`、`appliedAt=2026-07-24 14:39:48`；页面显示 `2026-07-24 14:39`。
- 甘特图接口仅返回来源生产工单号 `881MO093613`、`881MO093615`；页面折叠第一个工单后，真实 UI 显示仅有两个根工单。

## Requirement Checklist

- [x] 重排成功。
- [ ] 只有来源生产工单号为 `881MO093613`、`881MO093615` 的两个排产工单产品编号变橙色。
- [x] 最近一次成功排产时间更新为本次排产时间。
- [x] 生产排产页签甘特图有且仅有这两个工单。

## Finding

目标 b 失败。重排完成后，两个目标产品编号仍为黑色，DOM class 为 `schedule-order-pool__product-code--unscheduled`，计算颜色为 `rgb(23, 32, 51)`；页面预期的橙色 class 为 `schedule-order-pool__product-code--scheduled`。

前端 [index.vue](E:/IntRuoyi/IntRuoyiFronted/src/views/mes/pro/scheduleorder/index.vue:3386) 定义了 `updateLastReplanParticipatingScheduleOrders()`，但真实应用重排路径未调用它。因此 `lastReplanParticipatingScheduleOrderIds` 为空，`getScheduleOrderProductCodeClass()` 始终返回未排产黑色样式。

## Result

本次验收不通过。a、c、d 已通过；b 未通过，需修复产品编号参与重排后的状态更新并按相同真实路径复测。

## Closeout

临时 Playwright 脚本和截图/JSON 证据已按任务收尾规则删除；本报告保留了关键操作、时间、接口和页面断言结果。
