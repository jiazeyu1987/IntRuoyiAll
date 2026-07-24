# 任务：展厅封面失败项补齐 live preview asset（前端）

## Goal

记录本次 preview asset 数据修复后，前端用户可见状态与后端运行态是否重新对齐。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-showroom-cover-failed-items-preview-asset-repair\**`

## Non-Scope

- 不修改前端业务代码。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-showroom-batch-cover-failed-items-diagnosis\task.md`
- Status before this task: `Completed`
- Impact: 上一任务已确认前端错误提示与后端失败项一致，不阻塞本次数据修复后继续验证。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Current state: 仓库存在并行未提交改动。
- Impact: 本任务只新增修复记录，不覆盖无关改动。

## Milestones

1. 记录修复前的用户可见状态。
2. 对齐后端 preview asset 修复结果。
3. 执行 closeout preview。

## Expected Verification

- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260522-showroom-cover-failed-items-preview-asset-repair --mode preview`

## Current Status

Completed.

## Completed Work

- 对齐了后端 preview asset 修复结果：
  - 当前一键封面任务中已不再存在 `live product preview asset` 缺失导致的失败项
  - 前端后续看到的封面任务失败背景将不再包含这一类前置缺失
- 记录了当前前端入口仍会继续如实反映剩余后台任务状态。

## Verification Result

- PASS: backend data repair alignment
  - preview-asset-related cover failures reduced to `0`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260522-showroom-cover-failed-items-preview-asset-repair --mode preview`

## Final Assessment

- 这次前端无需改代码。
- 用户当前看到的封面任务状态后续若仍有失败，将不再是 `live product preview asset` 缺失这一类问题。
