# 20260729 merge worktrees into int_main

## Task Goal

查看当前 IntRuoyi Git worktree 数量，逐个检查附加 worktree 的提交状态，并将可安全合并的 worktree 分支融合到 `int_main`。

## Milestones

- [x] 读取并记录 worktree / Git / 收尾规则与适用经验门禁
- [x] 盘点主工作区与附加 worktree 列表、分支、状态
- [ ] 逐个处理附加 worktree：提交未提交变更，验证分支状态
- [ ] 将可安全合并的分支按顺序合并到 `int_main`
- [ ] 运行必要验证与分支运行端口门禁
- [ ] 完成任务收尾、记录最终结果

## Expected Verification

- `git worktree list --porcelain`
- 每个 worktree 的 `git status --short --branch`
- 每次提交后的 `git show --name-status --oneline -1`
- 合并前后的 `git status --short --branch`
- `scripts\preflight\branch-runtime-port-guard.ps1`

## Current Status

in_progress

## Inventory Summary

- Registered worktrees: 11 total.
- Main workspace: `E:\IntRuoyi`, branch `int_main`.
- Attached worktrees: 10 under `D:\IntRuoyiWorktree\`.
- Initial blocker/risk: local `int_main` was `ahead 1, behind 3` after the required dirty baseline commit; first `git fetch origin int_main` failed with GitHub connection reset.

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
