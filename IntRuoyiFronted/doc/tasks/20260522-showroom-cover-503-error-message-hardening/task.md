# 任务：展厅封面 503 错误信息收敛（前端）

## Goal

记录本次后端错误信息收敛后，前端用户最终看到的封面失败提示将更接近真实上游 `503` 原因。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-showroom-cover-503-error-message-hardening\**`

## Non-Scope

- 不修改前端业务代码。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-showroom-cover-failed-items-preview-asset-repair\task.md`
- Status before this task: `Completed`
- Impact: 上一任务已修复 preview asset 缺失问题，不阻塞本次继续收敛 503 错误信息。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Current state: 仓库存在并行未提交改动。
- Impact: 本任务只新增记录，不覆盖无关改动。

## Milestones

1. 记录本次错误信息收敛目标。
2. 对齐后端定向验证结论。
3. 执行 closeout preview。

## Expected Verification

- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260522-showroom-cover-503-error-message-hardening --mode preview`

## Current Status

Completed.

## Completed Work

- 已记录本次后端错误信息收敛目标与结果。
- 当前前端无需改代码；等后端新构建加载后，用户看到的封面失败提示会更接近真实上游 `503` 原因。

## Verification Result

- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260522-showroom-cover-503-error-message-hardening --mode preview`

## Final Assessment

- 前端无需改代码。
- 后端错误信息修复后，用户将不再看到 `Illegal char` 噪音前缀。
