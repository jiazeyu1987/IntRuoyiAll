# 任务：展厅封面 503 错误直接跳过（前端）

## Goal

记录本次后端把封面 `503 Service temporarily unavailable` 归类为“直接跳过”后的前端影响。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-showroom-cover-skip-503-errors\**`

## Non-Scope

- 不修改前端业务代码。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-showroom-cover-503-error-message-hardening\task.md`
- Status before this task: `Completed`
- Impact: 上一任务已确认 503 错误信息收敛，不阻塞本次继续记录“503 直接跳过”行为。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Current state: 仓库存在并行未提交改动。
- Impact: 本任务只新增记录，不覆盖无关改动。

## Milestones

1. 记录后端 503 跳过策略的前端影响。
2. 执行 closeout preview。

## Expected Verification

- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260522-showroom-cover-skip-503-errors --mode preview`

## Current Status

Completed.

## Completed Work

- 已记录后端 503 跳过策略变更对前端的影响：
  - 同类封面 503 将不再被后台重复放回等待队列
  - 前端后续看到的剩余待处理数量会更贴近真正还能继续处理的产品

## Verification Result

- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260522-showroom-cover-skip-503-errors --mode preview`
