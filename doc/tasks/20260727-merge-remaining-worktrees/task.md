# 20260727 Merge Remaining Worktrees

## Task Goal

按用户要求融合剩余目标 worktree：先将 `D:\IntRuoyiWorktree\20260727-todo-task-hidden-status` 的 dirty 内容在原分支形成可追溯提交，再尝试将 `codex/20260727-todo-task-hidden-status` 与 `codex/codex-test-process-route` 合入 `int_main`，验证通过后删除已融合 worktree。

## Milestones

- [x] 读取 worktree、Git、PowerShell、端口和收尾门禁。
- [x] 冻结主工作区既有脏状态为独立 baseline commit。
- [ ] 创建任务记录并记录适用经验门禁。
- [ ] 提交并验证 `todo-task-hidden-status` dirty 内容。
- [ ] 逐分支 merge 到 `int_main`，处理可安全解决的冲突。
- [ ] 运行聚焦回归与端口 guard。
- [ ] 删除已验证合入的 worktree 并释放端口登记。
- [ ] cleanup、提交、推送并记录最终证据。

## Expected Verification

- `git status --short --branch` in main and target worktrees.
- `git merge-tree --write-tree HEAD <branch>` before merge.
- `git merge-base --is-ancestor <branch> HEAD` after merge.
- Target task verification commands found from worktree task documents.
- `scripts\preflight\branch-runtime-port-guard.ps1`.
- `git worktree list --porcelain` and `Test-Path` after removal.

## Current Status

in_progress

## Design Constraint Check

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，先在原 worktree 提交 dirty 内容，再通过 Git merge 与冲突验证融合。
- `是否存在临时补丁或绕过`：否。

## Experience Gates

- `docs\worktree-memory.md#多 Worktree 批量融合门禁`：dirty 内容必须在原分支形成独立可追溯提交；冲突修复后运行该分支目标测试；不能证明 `merge-base --is-ancestor` 时不得删除 worktree。
- `docs\worktree-memory.md#Worktree 删除门禁`：删除前确认路径、clean 状态和已合入状态，删除后复核 Git 注册、物理目录和端口登记项。
