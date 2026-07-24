# 任务：展厅一键封面失败项诊断（后端）

## Goal

诊断当前一键封面后台任务 `id=2` 中 `FAILED=10` 的失败项，定位失败产品、失败原因与是否可重试，明确下一步处理建议。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-showroom-batch-cover-failed-items-diagnosis\**`

## Non-Scope

- 不修改业务代码，除非诊断结果已经明确且用户后续要求修复。
- 不手工篡改任务表结果。
- 不重置或删除现有批任务状态。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-showroom-batch-cover-runtime-concurrency-check\task.md`
- Status before this task: `Completed`
- Impact: 上一任务已确认一键封面 8 路并发生效，不阻塞本次继续诊断失败项。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Current state: 仓库存在并行未提交改动。
- Impact: 本任务只新增诊断记录，不覆盖无关改动。

## Milestones

1. 读取任务 `id=2` 的失败项明细与最近日志。
2. 对失败原因进行分组，判断是否可重试。
3. 输出明确结论和下一步建议。
4. 更新证据并执行 closeout preview。

## Expected Verification

- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-showroom-batch-cover-failed-items-diagnosis --mode preview`

## Current Status

Completed.

## Completed Work

- 读取了任务 `id=2` 的失败项明细，共 `10` 条。
- 提取了每条失败产品的 `productId / productCode / nameCn / attemptCount / lastError / lastAttemptAt`。
- 确认 10 条失败项的 `lastError` 完全一致，都是：
  - `SHOWROOM_TARGET_NOT_FOUND: live product preview asset is required`
- 结合当前运行态判断：
  - 这 10 条不是 Codex CLI 并发问题；
  - 也不是随机外部服务失败；
  - 是同一类业务前置缺失导致的确定性失败。

## Verification Result

- PASS: failed item inspection on `showroom_product_cover_batch_task_item where task_id=2 and status='FAILED'`
  - result count: `10`
  - all failed items share the same `lastError`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-showroom-batch-cover-failed-items-diagnosis --mode preview`

## Final Assessment

- 当前 `10` 个失败项的根因一致：
  - 这些产品缺少 `live product preview asset`
- 这类失败项当前不会因为后台继续续跑而自动恢复，除非先补齐对应产品的 live preview asset，或者后端逻辑改为不再依赖该前置。
- 当前一键封面任务 `id=2` 的并发 `8` 路运行已生效，但会继续跳过/保留这些失败项。
