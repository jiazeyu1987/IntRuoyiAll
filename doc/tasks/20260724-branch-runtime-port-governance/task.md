# Branch Runtime Port Governance

## Task Goal

Establish durable branch-specific local runtime port rules for `int_main`, `int_batch`, `int_shedule`, and `int_qms`, including worktree port derivation and merge/commit protection so branch runtime settings are not overwritten by later merges. The primary local `int_main` repository is `D:\ProjectPackage\IntRuoyi\IntRuoyiAll`.

## Milestones

- [x] Define the branch runtime port matrix and worktree derivation rules.
- [x] Add branch-specific frontend runtime env files and root startup scripts.
- [x] Add merge/commit/push guard scripts and install local Git hook routing.
- [x] Apply the same governance package to BatchRecord, Shedule, and QMS workspaces.
- [x] Verify guard behavior and record usage instructions.

## Expected Verification

- `scripts/preflight/branch-runtime-port-guard.ps1` passes in each branch workspace.
- The guard confirms `int_main=8081/48081`, `int_batch=8041/48041`, `int_shedule=8021/48021`, and `int_qms=8061/48061`.
- The frontend branch env files and root startup scripts preserve branch ports without changing `int_main` defaults.
- Local Git hook routing points to `.githooks` in each target workspace.

## Current Status

ready_for_closeout

Implementation and guard verification are complete. Commit/push closeout is not performed in this turn because each workspace has pre-existing unrelated tracked deletions that must not be silently mixed into this task.

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；通过端口矩阵、脚本入口、worktree 派生规则和 Git hooks 门禁固化。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

### Branch runtime port governance

- Trigger: 本机多分支并行运行、创建 worktree、合并 `int_main`、提交或推送端口相关配置。
- Preflight check: 运行 `scripts/preflight/branch-runtime-port-guard.ps1`，确认分支端口矩阵、前端 env、启动脚本、运行态文档和 hooks 入口存在且一致。
- Blocker: 当前分支/路径无法映射到端口画像，端口矩阵缺失，分支 env 被改回 `8081/48081`，或 hooks 未安装。
- Verification: 三个目标工作区 guard 均通过，并记录 `git config core.hooksPath`。
- Forbidden action: 禁止直接修改 `int_main` 默认端口，禁止用随机端口或静默换端口替代分支端口矩阵。
- Evidence: `docs/local-runtime.md`、`docs/worktree-restrictions.md`、本任务 `verification-report.md`。
