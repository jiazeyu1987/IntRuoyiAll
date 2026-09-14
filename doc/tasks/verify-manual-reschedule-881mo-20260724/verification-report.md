# Verification Report

## Objective

在本机 `http://127.0.0.1:8081`，使用用户授权的 `芋道源码/admin` 身份标签，执行来源生产工单号为 `881MO093613`、`881MO093615` 的两个排产工单真实页面手动重排，并核验四项目标。

## Status

PASS

## Evidence

- Playwright 真实页面路径：排产工单页签 -> 勾选两个目标工单 -> 手动重排 -> 开始重排 -> 选择 `2026-07-25` -> 确认应用重排。
- 修复后验证时间：2026-07-24 17:32（本机时间）。
- 页面成功提示：应用重排成功，新增任务 `136` 个、删除任务 `136` 个、保留任务 `7` 个。
- 应用请求只包含 `scheduleOrderIds=[131,127]`，对应来源生产工单号 `881MO093613`、`881MO093615`。
- 最近成功排产接口：`operationType=REPLAN_APPLY`、`appliedAt=1784885520000`；页面显示 `2026-07-24 17:32`。
- 产品编号高亮：仅 `881MO093613`/`YXN.069.001.1013` 与 `881MO093615`/`YXN.069.001.1002` 具有 `schedule-order-pool__product-code--scheduled`，计算颜色为 `rgb(212, 107, 8)`。
- 甘特图接口与折叠后 UI 仅返回/显示来源生产工单号 `881MO093613`、`881MO093615`。
- 证据文件：`output/playwright/verify-manual-reschedule-881mo-20260724-repair/repair-verification-report.json`、`before-replan-schedule-order.png`、`after-replan-product-code.png`、`after-replan-gantt.png`。

## Requirement Checklist

- [x] 重排成功。
- [x] 只有来源生产工单号为 `881MO093613`、`881MO093615` 的两个排产工单产品编号变橙色。
- [x] 最近一次成功排产时间更新为本次排产时间。
- [x] 生产排产页签甘特图有且仅有这两个工单。

## Finding

初次真实验证中目标 b 失败：重排完成后，两个目标产品编号仍为黑色，DOM class 为 `schedule-order-pool__product-code--unscheduled`，计算颜色为 `rgb(23, 32, 51)`；页面预期的橙色 class 为 `schedule-order-pool__product-code--scheduled`。

前端 [index.vue](E:/IntRuoyi/IntRuoyiFronted/src/views/mes/pro/scheduleorder/index.vue:3386) 定义了 `updateLastReplanParticipatingScheduleOrders()`，但真实应用重排路径未调用它。因此 `lastReplanParticipatingScheduleOrderIds` 为空，`getScheduleOrderProductCodeClass()` 始终返回未排产黑色样式。

修复后，`confirmApplyReplanStartChoice()` 在 `replanApply` 成功之后使用本次 `freshPreview` 更新最近一次实际重排参与工单集合，避免预览或失败路径提前标橙。

## Result

本次修复后验收通过。a、b、c、d 四项目标全部通过。

## Closeout

功能修复和真实路径验证已完成；当前任务进入 `ready_for_closeout`，等待二次收尾、提交与推送处理。
