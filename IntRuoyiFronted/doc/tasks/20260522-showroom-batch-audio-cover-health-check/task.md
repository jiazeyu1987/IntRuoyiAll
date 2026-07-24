# 任务：展厅产品一键语音与一键封面健康检查（前端）

## Goal

独立检查 `showroom/product` 的一键语音与一键封面当前是否存在明显问题，包括页面入口、状态反馈、运行态可见性与已有验证证据是否一致。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-showroom-batch-audio-cover-health-check\**`

## Non-Scope

- 不新增前端业务改动，除非检查中确认存在明确缺陷且用户后续要求修复。
- 不主动触发真实一键语音或一键封面批处理任务。
- 不伪造通过结论。

## Previous Task Check

- Previous same-repo task records:
  - `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-showroom-product-batch-audio-auto-check\task.md`
  - `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-showroom-product-narration-restart-and-monitor\task.md`
- Status before this task: `Completed`
- Impact: 前序相关任务已完成，不阻塞本次独立健康检查。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Current state: 仓库存在并行未提交改动。
- Impact: 本任务仅新增验证记录，不覆盖无关改动。

## Milestones

1. 读取现有任务文档、状态接口与运行态证据。
2. 对一键语音、一键封面分别形成“正常 / 异常 / 证据不足”判断。
3. 更新验证记录并执行 closeout preview。

## Expected Verification

- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260522-showroom-batch-audio-cover-health-check --mode preview`

## Current Status

Completed.

## Completed Work

- 复核了一键语音与一键封面的前端历史任务记录、当前页面契约与后端运行态。
- 复跑了前端定向源码回归：
  - `scripts/showroom-admin-product-list.test.mjs`
  - `scripts/showroom-admin-frontend.test.mjs`
  - `scripts/showroom-admin-batch-cover-auto-resume.test.mjs`
  - `scripts/showroom-admin-batch-cover-mode.test.mjs`
- 结论：
  - 一键语音：前端承接链路正常，但当前运行态存在真实后端/TTS 失败。
  - 一键封面：前端承接链路正常，且未发现当前活动任务异常。

## Verification Result

- PASS: `node --test scripts/showroom-admin-product-list.test.mjs scripts/showroom-admin-frontend.test.mjs scripts/showroom-admin-batch-cover-auto-resume.test.mjs scripts/showroom-admin-batch-cover-mode.test.mjs`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260522-showroom-batch-audio-cover-health-check --mode preview`

## Final Assessment

- 一键语音：有问题。页面显示逻辑不是主因，但当前后端自动检查状态显示 `failedCount` 与 `remainingActionableCount` 同步堆积，说明真实生成未成功推进。
- 一键封面：暂未发现问题。最近批任务已完成，无活动任务残留，前端承接测试也通过。
