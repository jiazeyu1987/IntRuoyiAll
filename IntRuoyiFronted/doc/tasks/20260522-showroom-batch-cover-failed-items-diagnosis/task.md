# 任务：展厅一键封面失败项诊断（前端）

## Goal

配合后端失败项诊断，记录当前前端入口状态与用户可见反馈是否一致。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-showroom-batch-cover-failed-items-diagnosis\**`

## Non-Scope

- 不修改前端业务代码。
- 不新增测试专用控件。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-showroom-batch-cover-runtime-concurrency-check\task.md`
- Status before this task: `Completed`
- Impact: 上一任务已确认前端入口能命中真实接口，不阻塞本次仅记录失败项诊断结论。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Current state: 仓库存在并行未提交改动。
- Impact: 本任务只新增诊断记录，不覆盖无关改动。

## Milestones

1. 记录前端已暴露的真实任务状态。
2. 汇总后端失败项结论到前端任务记录。
3. 执行 closeout preview。

## Expected Verification

- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260522-showroom-batch-cover-failed-items-diagnosis --mode preview`

## Current Status

Completed.

## Completed Work

- 复核了前端一键封面入口当前可见反馈：
  - 再次点击时，页面会原样提示存在未完成后台任务 `id=2`，剩余 `124` 个产品待生成。
- 对齐后端失败项诊断结果，确认当前前端并未吞错：
  - `10` 个失败项的根因一致，都是缺少 `live product preview asset`。

## Verification Result

- PASS: front-end visible behavior remains aligned with backend
  - repeated `一键封面` request is rejected with the real backend message about unfinished task `id=2`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260522-showroom-batch-cover-failed-items-diagnosis --mode preview`

## Final Assessment

- 前端入口本身没有新问题。
- 当前用户可见的“一键封面仍有未完成任务”提示与后端真实状态一致。
- 真正需要处理的是后端失败项所依赖的 `live product preview asset` 前置缺失。
