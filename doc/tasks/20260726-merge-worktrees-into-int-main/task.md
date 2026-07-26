# Task: Merge Worktrees Into int_main

## Task Goal

将 `D:\IntRuoyiWorktree\` 下当前登记的 IntRuoyi worktree 分支安全合入 `int_main`，完成必要验证后删除对应 worktree 目录，并保留可追溯的任务证据。

## Milestones

1. `completed`：读取强制规则，建立基线，核对主工作区与各 worktree 状态。
2. `completed`：按分支顺序合入 `int_main`，处理并验证冲突。
3. `completed`：验证合并结果、远端同步状态、worktree 删除结果及端口登记状态。
4. `completed`：执行收尾清理，提交收尾记录并完成任务。

## Expected Verification

- 所有待处理 worktree 分支均已明确合入、已知无需合入或因真实冲突阻塞。
- `int_main` 的合并结果通过必要的 Git 一致性检查。
- `int_main` 的本地提交已推送到 `origin/int_main`，且不再 ahead。
- 目标 worktree 删除后，`git worktree list` 与文件系统状态一致。
- 任务记录使用 UTF-8 保存，记录命令、结果、提交和阻塞项。

## Experience Gate Summary

- 删除前逐项确认目标绝对路径位于 `D:\IntRuoyiWorktree\`、Git 注册有效、分支提交已合入且 dirty 变更已正式保存。
- dirty worktree 不得静默丢弃；本任务目标是合入，因此 dirty 变更必须先形成可追溯提交并通过相应验证，否则阻塞。
- 优先使用 `git worktree remove`；删除后同时验证 `git worktree list --porcelain`、`Test-Path` 和端口登记表。
- 收尾状态先进入 `ready_for_closeout`，task-closeout preview/apply 通过后才能标记 `completed`。

## Current Status

completed

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否；缺少前置条件或存在无法安全判断的冲突时直接阻塞。
- 是否从根因和长期维护角度解决：是；使用 Git 正式合并与 worktree 管理，不复制文件或绕过分支历史。
- 是否存在临时补丁或绕过：否。
