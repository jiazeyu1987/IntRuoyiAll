# 任务：展厅一键语音 token 修复后复测（后端）

## Goal

在用户确认修复阿里云 NLS token 过期后，重新验证一键语音当前运行态是否恢复正常，并顺带确认一键封面仍保持健康。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-showroom-batch-audio-token-fix-retry\**`

## Non-Scope

- 不修改后端业务代码。
- 不主动修改一键封面或一键语音实现。
- 不用 mock 代替真实结果。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-showroom-batch-audio-cover-health-check\task.md`
- Status before this task: `Completed`
- Impact: 上一任务已定位语音失败来源与封面健康基线，不阻塞本次修复后复测。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Current state: 仓库存在并行未提交改动。
- Impact: 本任务只新增验证记录，不覆盖无关改动。

## Milestones

1. 读取当前一键语音与一键封面状态基线。
2. 重试一键语音并观察状态变化。
3. 形成“已恢复 / 仍异常”的最终判断。
4. 更新证据并执行 closeout preview。

## Expected Verification

- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-showroom-batch-audio-token-fix-retry --mode preview`

## Current Status

Completed.

## Completed Work

- 复查了 token 修复后的运行态基线。
- 使用真实测试租户鉴权重新触发了一轮一键语音批处理。
- 复查状态接口确认一键语音已恢复：成功生成 17 条、失败 0、自动检查关闭。
- 再次确认一键封面最近批任务保持完成态、无新失败。

## Verification Result

- PASS: `POST /admin-api/showroom/product/batch-generate-narration-audio`
  - returned `matchedCount=180`、`publishedCount=54`、`skippedExistingCount=37`、`succeededCount=17`、`failedCount=0`、`autoCheckEnabled=false`
- PASS: `GET /admin-api/showroom/product/batch-generate-narration-audio-state`
  - returned `enabled=false`、`succeededCount=17`、`failedCount=0`、`remainingActionableCount=0`
- PASS: latest one-click cover task remains healthy
  - latest `showroom_product_cover_batch_task.id=1` is `COMPLETED` with `failedCount=0`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-showroom-batch-audio-token-fix-retry --mode preview`

## Final Assessment

- 一键语音：已恢复正常，token 修复后重试成功。
- 一键封面：继续保持正常。
