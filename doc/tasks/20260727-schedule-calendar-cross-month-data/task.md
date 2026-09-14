# 20260727-schedule-calendar-cross-month-data

## Task Goal

修复排程日历月视图跨月日期显示为任务 0 / 工单 0 的问题，确保可见 42 天网格中的相邻月份日期也使用排程日历自己的正式接口数据，不复用甘特图接口。

## Milestones

- [x] 记录 BDD 场景与现有问题证据
- [x] 增加前端静态回归契约并先确认 RED
- [x] 修复月视图可见日期的数据加载与合并逻辑
- [x] 运行目标回归验证并记录结果

## Expected Verification

- 目标静态契约先失败于“只加载当前月 / 跨月格子缺少 info”。
- 修复后目标静态契约通过，证明月视图会加载可见网格涉及的所有月份，并合并相邻月日期数据。

## Current Status

ready_for_closeout

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，修复月视图可见范围与已加载月份范围不一致的根因。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

- 前端静态契约隔离门禁：本任务新增聚焦静态契约覆盖当前行为，不修改无关大契约，不用全量历史失败掩盖当前需求。

## Verification Evidence

- `node tests/e2e/mes-schedule-calendar-visible-months-static.spec.js` -> RED，失败于缺少 `visibleMonthDays`，证明当前实现只加载当前月数据。
- `pnpm e2e:mes:schedule-calendar-visible-months:static` -> PASS。
- `pnpm ts:check:schedule` -> PASS。
- `git diff --check -- <task-owned files>` -> PASS。

## Closeout Notes

- 实现与验证已完成。
- 未执行提交/推送：主工作区存在大量本任务开始前的非本任务脏改动，本次未将无关改动做基线提交，避免混入当前修复交付。
