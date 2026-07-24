# Task: Refresh Auto-Schedule Scope Before Preview

## Goal

修复排程日历与生产排产页面中“生成预览”依赖陈旧 `scopeWorkOrderIds` 的问题，避免页面在工单范围已变化但前端未刷新时误提示“当前没有可参与自动排产的已确认自制工单”或“当前筛选范围没有可排产工单”。

## Scope

- 先创建当前前端任务文档、执行日志与回归验证脚本。
- 用最小前端契约验证证明点击预览前还没有主动刷新最新 scope。
- 在 `schedule-calendar` 与 `task` 两个入口做最小修复。
- 运行定向静态验证与真实 Playwright 回归，确认现有预览链路不回退。

## Previous Task Check

- Previous frontend task: `doc/tasks/20260517T220502-schedule-calendar-detail-route-workorder-links/task.md`
- Status before this task: blocked by higher-priority defect in the current turn.
- Impact: the paused schedule-calendar detail-links task does not block this preview-scope fix, but its unfinished code must remain isolated.

## Milestones

- [x] M1: Create task package before production edits.
- [x] M2: Add a failing regression verifier for stale scope before preview.
- [x] M3: Implement the smallest frontend fix in both preview entry points.
- [x] M4: Run targeted verification and record RED/GREEN evidence.
- [x] M5: Update task status, closeout notes, and commit only task-scoped frontend files.

## Expected Verification

- `node doc/tasks/20260517T223500-schedule-calendar-preview-scope-refresh/scripts/verify-preview-scope-refresh.cjs`
- `npm run ts:check`
- `npx.cmd --yes --package @playwright/cli playwright-cli --session schedule-calendar-preview-scope-refresh run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260517T223500-schedule-calendar-preview-scope-refresh\scripts\probe-schedule-calendar-preview-scope.mjs`

## Current Status

Completed. 前端已补上“点击预览前刷新最新 scope”的最小修复，任务级 RED/GREEN 校验、真实 Playwright 探针与全量 `npm run ts:check` 均已通过。

## Verification Summary

- PASS: `node doc/tasks/20260517T223500-schedule-calendar-preview-scope-refresh/scripts/verify-preview-scope-refresh.cjs`
- PASS: `npx.cmd --yes --package @playwright/cli playwright-cli --session schedule-calendar-preview-scope-refresh-fresh run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260517T223500-schedule-calendar-preview-scope-refresh\scripts\probe-schedule-calendar-preview-scope.mjs`
- PASS: `$env:NODE_OPTIONS='--max-old-space-size=8192'; npm run ts:check`

## Remaining Blockers

- None.
