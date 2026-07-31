# 20260729 merge worktrees into int_main

## Task Goal

查看当前 IntRuoyi Git worktree 数量，逐个检查附加 worktree 的提交状态，并将可安全合并的 worktree 分支融合到 `int_main`。

## Milestones

- [x] 读取并记录 worktree / Git / 收尾规则与适用经验门禁
- [x] 盘点主工作区与附加 worktree 列表、分支、状态
- [x] 逐个处理附加 worktree：提交未提交变更，验证分支状态
- [x] 将可安全合并的分支按顺序合并到 `int_main`
- [x] 运行必要验证与分支运行端口门禁
- [ ] 完成任务收尾、记录最终结果

## Expected Verification

- `git worktree list --porcelain`
- 每个 worktree 的 `git status --short --branch`
- 每次提交后的 `git show --name-status --oneline -1`
- 合并前后的 `git status --short --branch`
- `scripts\preflight\branch-runtime-port-guard.ps1`

## Current Status

blocked

## Inventory Summary

- Registered worktrees: 12 total.
- Main workspace: `E:\IntRuoyi`, branch `int_main`.
- Attached worktrees: 11 under `D:\IntRuoyiWorktree\`.
- Eligible result: 9 attached worktree branches are now ancestors of local `int_main`.
- Blocked result: 2 attached worktree branches were not merged because their own task evidence is blocked or dirty with unresolved E2E prerequisites:
  - `codex/20260727-route-flow-batch-record-form-source-e2e`
  - `codex/restart-int-main-latest-backend-20260727`
- Local blocker/risk: final completion cannot be marked while blocked worktree branches remain unmerged without explicit user override and formal prerequisites.

## Integration Summary

- Baseline commits preserved unrelated dirty task docs before merge commits: `8cf2c4f6`, `d27ca83a`, `b9187a11`.
- Current task startup docs were committed as `99005cfa`.
- `origin/int_main` snapshot was merged locally before integration as `cc64234d`.
- Dirty worktree implementation commits made on their original branches: `7354b8a5`, `07ad0955`, `e3ba7c96`.
- Merge commits into `int_main`: `d3fcbc7b`, `50453f4e`, `e9ea70a7`, `0acad930`, `0c584f71`.
- Experience consolidation updated existing long-term docs: `docs\worktree-memory.md`, `docs\release-build-preflight-lessons.md`, `docs\experience-index.md`.

## Blocked Worktrees

- `D:\IntRuoyiWorktree\20260727-route-flow-batch-record-form-source-e2e` is clean but remains `not-ancestor`; its task status is `blocked` because the exact `球囊扩张导管` route lacks formal per-process batch-record reports, and `PTCA球囊扩张导管` page verification lacks a test-tenant login path.
- `D:\IntRuoyiWorktree\20260727_int_main_latest_backend_runtime` is dirty and remains `not-ancestor`; its task status is `blocked_for_e2e_validation` because the frontend still references deprecated `/batch-record-cell-link/prefill` and real E2E lacks `LOCAL_DATABASE_FIXTURE`.
- `D:\IntRuoyiWorktree\20260728-codex-node-chain-first-node-contract` is already an ancestor, but still contains local runtime dirtiness at `.runtime\codex-test-runner\codex-runner.pid`; this artifact was intentionally excluded from the implementation commit.

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否
- `是否从根因和长期维护角度解决`：是
- `是否存在临时补丁或绕过`：否

## Applicable Experience Gates

- `docs\worktree-memory.md#多-worktree-批量融合门禁`：先冻结 `int_main` dirty 基线；逐个 worktree 记录 branch、HEAD、status；dirty 内容必须在原分支形成独立可追溯提交；按顺序合并并验证 ancestor。
- `docs\worktree-memory.md#并行主工作区远端快进融合门禁`：如本地主工作区无法 clean，需阻塞或走已验证的远端快进融合路径；不得把 dirty 主工作区清洁失败当成已集成。
- `docs\worktree-memory.md#d-main-本地主线滞后远端融合门禁`：合并前后记录 ahead/behind，合并后运行 branch runtime port guard。
- `docs\powershell-memory.md#脏工作区基线门禁`：提交前如存在既有脏改动，先形成独立基线提交并记录 hash 与文件清单。
- `docs\powershell-memory.md#github-推送大文件门禁`：推送前扫描待推送历史，不得强推或历史重写。
