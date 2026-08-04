# 20260804 生产组长内容独立页签

## Task Goal

将 eDHR 批记录中的生产组长内容从“组长工作台”拆出，改为使用专门“生产组长”页签展示；同时按最终口径保留 eDHR 内部“PQC组长”独立页签，PQC 使用 `/mes/pro/feedback/edhr-batch-pqc-leader` 与 `BatchPqcLeaderWorkbenchPage.vue`，不回退为 process-pool 独立路由口径。

## Milestones

- [x] 识别现有组长工作台与生产/PQC 组长内容边界
- [x] 编写并运行最小 RED 静态合同，证明隔离分支缺少 eDHR PQC组长包装页
- [x] 实现 eDHR 生产组长与 PQC组长双独立页签，并从组长工作台移除生产组长内容
- [x] 运行定向 GREEN/REGRESSION 验证并记录证据
- [ ] 完成收尾清理、提交和推送

## Expected Verification

- `workdir=IntRuoyiFronted; node tests/e2e/edhr-batch-record-leader-tabs-static.spec.js`
- `workdir=IntRuoyiFronted; node tests/e2e/edhr-batch-page-graph-tab-static.spec.js`
- `workdir=IntRuoyiFronted; node tests/e2e/mes-process-pool-team-leader-static.spec.js`
- `workdir=IntRuoyiFronted; pnpm ts:check`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260804-production-leader-tab/frontend-feature-evidence.md`

## Current Status

blocked

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否
- `是否从根因和长期维护角度解决`：是，按业务页签职责保留生产组长、PQC组长两个 eDHR 独立页签，并让组长工作台不再承载生产组长内容。
- `是否存在临时补丁或绕过`：否

## Applicable Experience Gates

- `docs/frontend-development.md#前端静态契约隔离门禁`：本任务用聚焦静态合同先 RED/GREEN，避免被无关全量检查阻塞时误判。
- `docs/e2e-rules.md#windows-换行与脚本行为同步`：修改 `tests/e2e/*static.spec.js` 时，静态合同需使用稳定源码片段和路由名断言。
- `docs/powershell-memory.md#共享分支并发基线提交门禁`：共享工作区出现相反 process-pool 口径，当前实现保留在隔离 worktree，避免继续覆盖 `E:\IntRuoyi`。
- `docs/powershell-memory.md#脏工作区基线门禁`：隔离 worktree 当前仅包含本任务自有变更，提交需显式暂存任务文件。

## Cleanup Candidates

- doc/tasks/20260804-production-leader-tab/frontend-feature-evidence.md

## Final Verification

- RED: `node tests\e2e\edhr-batch-record-leader-tabs-static.spec.js` -> FAIL，expected `src/views/mes/pro/edhr-batch/BatchPqcLeaderWorkbenchPage.vue must exist.`
- PASS: `node tests\e2e\edhr-batch-record-leader-tabs-static.spec.js`
- PASS: `node tests\e2e\edhr-batch-page-graph-tab-static.spec.js`
- PASS: `node tests\e2e\mes-process-pool-team-leader-static.spec.js`
- PASS: `pnpm ts:check`
- PASS: frontend feature evidence validator before cleanup.
- PASS: stale `int_main` slot 4 was released after user approval because its physical path was missing and absent from `git worktree list`; current worktree is registered as slot 4 (`8085/48085`).
- PASS: task-closeout cleanup preview/apply with `--worktree-closeout off` deleted only temporary `frontend-feature-evidence.md` and kept core records.
- PASS: pushed branch `codex/production-leader-tab-20260804` to `origin`.
- BLOCKED: automatic linked-worktree closeout preview cannot proceed because current branch cannot be fast-forward merged into `int_main`, and main worktree `E:\IntRuoyi` is dirty.
- NOTE: shared `E:\IntRuoyi` currently contains unrelated/concurrent route-tab changes and remains unsafe for broad staging; this branch is the verified isolated implementation.
