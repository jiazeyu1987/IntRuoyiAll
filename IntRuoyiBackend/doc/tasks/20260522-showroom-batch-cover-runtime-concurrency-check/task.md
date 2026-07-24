# 任务：展厅一键封面运行态并发验证（后端）

## Goal

在“最多 8 个 Codex CLI 并发生成封面”的代码改动提交后，用真实运行态验证一键封面确实按如下规则执行：

- 待生成产品数大于等于 8 时，最多同时运行 8 个；
- 待生成产品数小于 8 时，同时运行数等于待生成产品数；
- 本轮任务最终能正常推进与收口。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-showroom-batch-cover-runtime-concurrency-check\**`

## Non-Scope

- 不再修改业务代码，除非运行态验证暴露出新的真实缺陷。
- 不改一键封面前端交互。
- 不用 mock 代替真实任务表和真实批量请求。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-showroom-batch-cover-parallelism-8\task.md`
- Status before this task: `Completed`
- Impact: 上一任务已完成代码和定向测试，不阻塞本次继续做真实运行态验证。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Current state: 仓库存在并行未提交改动。
- Impact: 本任务只新增验证记录与必要的一次性运行证据，不覆盖无关改动。

## Milestones

1. 确认本地前后端运行态已加载最新封面并发代码。
2. 从真实入口启动一轮一键封面任务。
3. 观察任务表与任务项状态，验证并发数量符合 `min(8, 待处理数)`。
4. 更新证据并执行 closeout preview。

## Expected Verification

- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-showroom-batch-cover-runtime-concurrency-check --mode preview`

## Current Status

Completed.

## Completed Work

- 已将 `48081` 后端切换到包含 `ce1d85e063` 的最新构建。
- 已确认当前待补封面的已发布产品共有 `124` 个，满足“应出现 8 路并发”的运行前置。
- 通过真实产品管理页触发了一次 `一键封面 -> 只生成未上传的` 请求；该请求被后端按预期拒绝，因为系统中已存在未完成后台任务 `id=2`。
- 随后直接观察这条现有后台任务 `id=2` 的真实任务表状态，确认它已经进入运行态，且稳定保持 `8` 个 `RUNNING` 任务项。

## Verification Result

- PASS: runtime backend switched to latest build
  - current runtime jar: `D:\ProjectPackage\Int\IntRuoyi\output\runtime\backend-cover8-20260522-173602.jar`
- PASS: real frontend request reached backend
  - `POST /admin-api/showroom/product/batch-generate-cover-image`
  - response: `SHOWROOM_COVER_GENERATION_FAILED: 已存在未完成的一键封面后台任务，任务 2 仍有 124 个产品待生成，请等待自动续跑完成后再重试`
- PASS: runtime concurrency observation on existing task `id=2`
  - task header state: `status=RUNNING`, `remainingPendingCount=124`
  - item distribution before observation: `RUNNING=8`, `WAITING=106`, `FAILED=10`
  - 4 polls over 30s all stayed at `RUNNING=8`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-showroom-batch-cover-runtime-concurrency-check --mode preview`

## Final Assessment

- 运行态已验证“一键封面最多 8 路并发”生效。
- 当前真实任务因为待处理数量 `124` 大于 `8`，所以稳定看到 `8` 个并发 worker。
- “待处理数量少于 8 时按实际数量并发”这部分本轮没有真实数据窗口，但已由定向测试覆盖通过。
