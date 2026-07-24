# 任务：展厅产品一键语音与一键封面健康检查（后端）

## Goal

独立检查 `showroom/product` 一键语音与一键封面的当前运行态、任务状态持久化和定向回归证据，判断是否存在明显问题。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-showroom-batch-audio-cover-health-check\**`

## Non-Scope

- 不主动修改后端业务代码，除非检查中确认存在明确缺陷且用户后续要求修复。
- 不主动触发真实一键语音或一键封面批处理任务。
- 不用 mock 结果替代真实状态。

## Previous Task Check

- Previous same-repo task records:
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-showroom-product-batch-audio-auto-check\task.md`
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-showroom-product-batch-cover-auto-resume\task.md`
- Status before this task: `Completed / Blocked as recorded`
- Impact: 相关任务已有历史证据，可作为本次独立检查输入。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Current state: 仓库存在并行未提交改动。
- Impact: 本任务仅新增验证记录，不覆盖无关改动。

## Milestones

1. 读取现有任务文档、状态接口、任务表和运行态证据。
2. 对一键语音、一键封面分别形成“正常 / 异常 / 证据不足”判断。
3. 更新验证记录并执行 closeout preview。

## Expected Verification

- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-showroom-batch-audio-cover-health-check --mode preview`

## Current Status

Completed.

## Completed Work

- 读取了一键语音状态接口与一键封面批任务表，核对当前真实运行态。
- 复跑了后端定向回归：
  - `ShowroomProductCoverBatchTaskServiceTest`
  - `ShowroomApiRuntimeBatchCoverModeTest`
  - `ShowroomProductNarrationRegressionTest`
- 结论：
  - 一键语音：当前运行态有真实问题，失败集中在阿里云 NLS TTS。
  - 一键封面：当前未发现活动任务异常，最近批任务已正常收口。

## Verification Result

- PASS: `mvn -pl yudao-module-showroom "-Dtest=ShowroomProductCoverBatchTaskServiceTest,ShowroomApiRuntimeBatchCoverModeTest,ShowroomProductNarrationRegressionTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-showroom-batch-audio-cover-health-check --mode preview`

## Final Assessment

- 一键语音：有问题。
  - 当前 `GET /admin-api/showroom/product/batch-generate-narration-audio-state` 返回：
    - `enabled=true`
    - `failedCount=40`
    - `remainingActionableCount=40`
    - `lastFailureMessage=SHOWROOM_AUDIO_GENERATION_FAILED: aliyun_nls_tts_failed status=400 ... status=40000001`
  - 说明批量语音自动检查还在运行，但实际 TTS 生成连续失败，没有完成推进。
- 一键封面：暂未发现问题。
  - 当前批任务表没有 `WAITING/RUNNING` 活动任务。
  - 最近任务 `showroom_product_cover_batch_task.id=1` 为 `COMPLETED`，`succeededCount=8`、`failedCount=0`、`remainingPendingCount=0`。
  - 最近 item 明细均为 `COMPLETED`，未见残留错误。
