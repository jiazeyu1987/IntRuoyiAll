# 任务：展厅一键语音 token 修复后复测（前端）

## Goal

在用户确认已修复语音 token 过期后，重新验证 `showroom/product` 的一键语音是否恢复正常，并顺带确认一键封面当前状态未受影响。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-showroom-batch-audio-token-fix-retry\**`

## Non-Scope

- 不修改前端业务代码。
- 不主动改动一键封面或一键语音实现。
- 不伪造验证结果。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-showroom-batch-audio-cover-health-check\task.md`
- Status before this task: `Completed`
- Impact: 上一任务已定位一键语音运行态失败与一键封面正常，不阻塞本次在 token 修复后继续复测。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Current state: 仓库存在并行未提交改动。
- Impact: 本任务只新增验证记录，不覆盖无关改动。

## Milestones

1. 读取当前一键语音与一键封面状态基线。
2. 通过真实产品管理页重试一键语音。
3. 观察状态是否恢复推进，并形成结论。
4. 更新证据并执行 closeout preview。

## Expected Verification

- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260522-showroom-batch-audio-token-fix-retry --mode preview`

## Current Status

Completed.

## Completed Work

- 复查了 token 修复后的运行态基线。
- 通过真实测试租户鉴权重新触发了一轮一键语音批处理。
- 复查状态接口确认一键语音已恢复：本轮成功生成 17 条，失败为 0，自动检查已关闭。
- 顺带再次确认一键封面最近任务仍为完成态，未受影响。

## Verification Result

- PASS: authenticated retry `POST /admin-api/showroom/product/batch-generate-narration-audio`
  - returned `matchedCount=180`、`publishedCount=54`、`skippedExistingCount=37`、`succeededCount=17`、`failedCount=0`、`autoCheckEnabled=false`
- PASS: `GET /admin-api/showroom/product/batch-generate-narration-audio-state`
  - returned `enabled=false`、`succeededCount=17`、`failedCount=0`、`remainingActionableCount=0`
- PASS: latest one-click cover task remains healthy
  - latest `showroom_product_cover_batch_task.id=1` remains `COMPLETED` with `failedCount=0`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260522-showroom-batch-audio-token-fix-retry --mode preview`

## Final Assessment

- 一键语音：已恢复正常。
- 一键封面：仍然正常。
